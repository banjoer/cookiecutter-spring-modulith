# Database and migration ownership

## One database, module-owned tables

PostgreSQL is the system of record. Modules share a database instance but never share ownership of a
table. There is no generic shared repository and no cross-module SQL.

| Module | Table | Migration |
| --- | --- | --- |
| identity | `identity_user_accounts` | `db/migration/identity/V100__identity_create_user_accounts.sql` |
| notifications | `notifications` | `db/migration/notifications/V200__notifications_create_notifications.sql` |
| audit | `audit_entries` | `db/migration/audit/V300__audit_create_audit_entries.sql` |

| messaging infrastructure | `messaging_outbox`, `messaging_inbox` | `db/migration/messaging/V400__messaging_create_outbox_and_inbox.sql` |

| Spring Modulith | `event_publication` | `db/migration/infrastructure/V900__infrastructure_event_publication.sql` |

## Version range convention

Flyway versions are global, even when files are grouped by ownership. Reserve a range per module so
new migrations do not collide:

| Owner | Range | Examples |
| --- | --- | --- |
| identity | V100–V199 | V101, V102 |
| notifications | V200–V299 | V201 |
| audit | V300–V399 | V301 |
| messaging infrastructure | V400–V499 | V401 |
| billing (example future module) | V500–V599 | V500 |
| shared infrastructure / Spring Modulith | V900–V999 | V901 |

Append the module name to the description: `V101__identity_add_display_name.sql`. The convention
makes ownership obvious in `flyway_schema_history` and code review.

## Flyway configuration

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

- Flyway runs before JPA validation.
- Hibernate never creates production tables.
- Hibernate validates the mapping against the migrated schema at startup.
- `open-in-view=false` keeps transactions at the service boundary and prevents lazy loading in web
  serialization.

## Spring Modulith event publication table

`db/migration/infrastructure/V900__infrastructure_event_publication.sql` contains the PostgreSQL schema from the Spring
Modulith reference appendix. Keep it aligned when upgrading Spring Modulith. Spring Modulith's JPA
registry uses the table to:

- record publications before listeners run;
- mark completion after successful listener transactions;
- expose incomplete and failed publications for retry;
- support staleness checks and resubmission.

If you upgrade to a Spring Modulith version with a changed schema, add a new migration; never edit an
already-applied migration.

## Module-aware Flyway (optional)

Spring Modulith 2.x can execute module-specific migrations in dependency order using separate
history tables. That requires a `db/migration/__root` folder plus `db/migration/<module>` folders and
`spring.modulith.runtime.flyway-enabled=true`. This template uses a single global Flyway history with per-module subfolders
(`db/migration/identity`, `db/migration/notifications`, and so on) plus global version ranges.
The simpler global history was chosen because:

- it is easy to understand and operate;
- it keeps a single migration history;
- it avoids surprises in `@ApplicationModuleTest` runs;
- it works identically in local, CI, and production.

You can adopt module-aware migrations later without changing module ownership.

## Adding a migration

1. Pick the next version in your module's range.
2. Write forward-only SQL with explicit constraints and indexes.
3. Test it against PostgreSQL using Testcontainers.
4. Do not modify an applied migration; add a new one.
5. Consider zero-downtime deployment: add nullable columns, backfill, then add constraints in a
   later migration.

Example:

```sql
ALTER TABLE identity_user_accounts
    ADD COLUMN display_name VARCHAR(120);

CREATE INDEX idx_identity_user_accounts_display_name
    ON identity_user_accounts (display_name);
```

## Testing migrations

`IdentityIntegrationTest` asserts that `flyway_schema_history` is populated and that the module table
exists. Context startup with `ddl-auto=validate` also verifies that every JPA mapping matches the
migrated schema.

Use a real PostgreSQL container, not H2. PostgreSQL-specific types (`UUID`, `TIMESTAMP WITH TIME
ZONE`, hash indexes) are part of the design.

## Schema evolution and rollback

Flyway Community is forward-only. For production:

- Take a backup or snapshot before risky migrations.
- Use backward-compatible changes and deploy in stages.
- Keep a tested restore procedure.
- Treat destructive migrations as separate, reviewed changes.

## Disabled modules and schema

Disabling a module does not drop its tables. This preserves data when a capability is temporarily
disabled. Removing a module permanently is a separate migration project with its own data-retention
and rollback plan.
