package {{ cookiecutter.package_name }}.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

/**
 * Marks Docker-backed integration tests so {@code ./gradlew test -PskipIntegrationTests} can skip
 * them on machines without Docker.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("integration")
@ActiveProfiles("test")
public @interface IntegrationTest {
}
