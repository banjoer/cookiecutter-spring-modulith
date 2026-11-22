package {{ cookiecutter.package_name }}.architecture;

import org.junit.jupiter.api.Test;

import org.springframework.modulith.core.ApplicationModules;

import {{ cookiecutter.package_name }}.Application;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fails the build if a module reaches into another module's internals or introduces a cycle.
 */
class ApplicationModulesTest {

    private final ApplicationModules modules = ApplicationModules.of(Application.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void discoversTheExpectedModules() {
        assertThat(modules.stream().map(module -> module.getBasePackage().getName()))
                .contains(
                        "{{ cookiecutter.package_name }}.identity",
                        "{{ cookiecutter.package_name }}.notifications",
                        "{{ cookiecutter.package_name }}.audit",
                        "{{ cookiecutter.package_name }}.messaging",
                        "{{ cookiecutter.package_name }}.infrastructure",
                        "{{ cookiecutter.package_name }}.bootstrap");
    }
}
