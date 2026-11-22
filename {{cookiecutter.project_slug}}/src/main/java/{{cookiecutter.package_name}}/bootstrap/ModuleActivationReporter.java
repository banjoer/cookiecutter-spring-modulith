package {{ cookiecutter.package_name }}.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs the effective module and integration state at startup.
 *
 * <p>This is deliberately a report, not an activation mechanism. Activation decisions are owned by
 * the configuration classes and their {@code @ConditionalOnProperty} conditions.</p>
 */
@Component
public class ModuleActivationReporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModuleActivationReporter.class);

    private final Environment environment;

    public ModuleActivationReporter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Module activation: identity={}, notifications={}, audit={}",
                enabled("app.modules.identity.enabled"),
                enabled("app.modules.notifications.enabled"),
                enabled("app.modules.audit.enabled"));
        log.info("Integration activation: rabbitmq={}, stomp={}",
                enabled("app.integrations.rabbitmq.enabled"),
                enabled("app.integrations.stomp.enabled"));
    }

    private boolean enabled(String property) {
        return environment.getProperty(property, Boolean.class, false);
    }
}
