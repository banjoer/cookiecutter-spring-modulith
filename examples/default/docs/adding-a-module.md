# Adding a module

This example adds a `billing` module with its own API, event, persistence, configuration, and
activation boundary. Replace names and business rules with your own.

## 1. Choose the module type

- **Core module** — always part of the product, imported explicitly by
  `bootstrap/ApplicationModulesConfiguration`.
- **Optional module** — registered through Spring Boot auto-configuration and enabled by
  `app.modules.billing.enabled`.

Most new capabilities should start as optional modules so they can be disabled without a code
change. A module should become core only when the application cannot function without it.

## 2. Create the package layout

```text
billing/
├── package-info.java
├── BillingProperties.java
├── BillingAutoConfiguration.java
├── api/
│   ├── package-info.java
│   ├── BillingApi.java
│   └── InvoiceView.java
├── events/
│   ├── package-info.java
│   └── InvoiceIssuedEvent.java
├── domain/
│   ├── Invoice.java
│   └── InvoiceRepository.java
├── application/
│   └── BillingService.java
└── web/
    └── BillingController.java
```

Declare the module and its allowed dependencies:

```java
@ApplicationModule(
        displayName = "Billing",
        allowedDependencies = {"identity :: events", "shared :: error"})
package com.example.springmodulithapplication.billing;
```

Expose only the packages other modules may use:

```java
@NamedInterface("api")
package com.example.springmodulithapplication.billing.api;

@NamedInterface("events")
package com.example.springmodulithapplication.billing.events;
```

Everything else is internal. Other modules must not import `Invoice`, `InvoiceRepository`, or
`BillingService`.

## 3. Add typed configuration

```java
@Validated
@ConfigurationProperties(prefix = "app.modules.billing")
public class BillingProperties {

    /** Whether the billing module is active. */
    private boolean enabled = true;

    /** ISO-4217 currency used when no currency is supplied. */
    @NotBlank
    private String defaultCurrency = "EUR";

    // getters and setters
}
```

Add the values under `app.modules.billing` in `application.yml`. Environment variables use relaxed
binding, for example `APP_MODULES_BILLING_DEFAULT_CURRENCY=USD`.

## 4. Create the activation boundary

For an optional module, create an auto-configuration and register it in
`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```java
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "app.modules.billing",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(BillingProperties.class)
@EnableJpaRepositories(basePackageClasses = InvoiceRepository.class)
@ComponentScan(basePackageClasses = {BillingService.class, BillingController.class})
public class BillingAutoConfiguration {
}
```

For a core module, import the configuration from `ApplicationModulesConfiguration` instead.

Do not add `@ComponentScan` for the module at the application root. Scoped scans inside the
activation boundary are what make runtime disabling reliable.

## 5. Own your persistence

Create a migration with the next version in the module's reserved range. For example, if identity
owns `V100–V199`, notifications `V200–V299`, audit `V300–V399`, and messaging infrastructure
`V400–V499`, give billing `V500`:

```sql
CREATE TABLE billing_invoices
(
    id         UUID PRIMARY KEY,
    user_id    UUID           NOT NULL,
    amount     NUMERIC(19, 2) NOT NULL,
    currency   VARCHAR(3)     NOT NULL,
    issued_at  TIMESTAMP WITH TIME ZONE NOT NULL
);
```

See [Database and migration ownership](database-migrations.md).

## 6. Publish a public event

```java
public record InvoiceIssuedEvent(
        UUID invoiceId,
        UUID userId,
        java.math.BigDecimal amount,
        java.time.Instant issuedAt) {
}
```

Publish it inside the transaction that persists the invoice:

```java
@Service
public class BillingService implements BillingApi {

    private final InvoiceRepository repository;
    private final ApplicationEventPublisher events;

    @Transactional
    public InvoiceView issue(UUID userId, BigDecimal amount, String currency) {
        Invoice invoice = repository.save(new Invoice(UUID.randomUUID(), userId, amount, currency, Instant.now()));
        events.publishEvent(new InvoiceIssuedEvent(
                invoice.getId(), invoice.getUserId(), invoice.getAmount(), invoice.getIssuedAt()));
        return toView(invoice);
    }
}
```

Other modules consume the event; they do not call `InvoiceRepository`.

## 7. Add tests

At minimum:

- A unit test for domain rules.
- An architecture check (the existing `ApplicationModulesTest` covers the new module automatically).
- An integration test with the module enabled and disabled.
- A test that verifies your migration runs on PostgreSQL.

```java
@SpringBootTest(properties = {
        "app.modules.billing.enabled=true",
        "app.modules.notifications.enabled=false",
        "app.modules.audit.enabled=false"
})
class BillingIntegrationTest extends AbstractPostgresIntegrationTest {
    // ...
}
```

The test support classes start one PostgreSQL container per test JVM.

## 8. Check the boundaries

```bash
./gradlew test --tests '*ApplicationModulesTest'
```

If another module accesses `billing.domain` or an undeclared package, the test fails with the exact
offending call path. Fix the dependency by exposing an API or event instead of weakening the module
declaration.

## Checklist

- [ ] Module package has an `@ApplicationModule` declaration.
- [ ] Public packages use `@NamedInterface`.
- [ ] Module owns its tables and migrations.
- [ ] Typed `@ConfigurationProperties` with validation.
- [ ] Activation boundary is a configuration class, not scattered conditions.
- [ ] No repository or entity is shared across modules.
- [ ] Events are used for secondary processing.
- [ ] Architecture, enabled, and disabled tests exist.
