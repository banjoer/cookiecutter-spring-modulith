package {{ cookiecutter.package_name }}.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Starts one PostgreSQL container per test JVM and wires it into the Spring environment. The
 * container is stopped by Testcontainers' Ryuk cleanup when the JVM exits.
 */
public abstract class AbstractPostgresIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("application")
                    .withUsername("application")
                    .withPassword("integration-test-password");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
