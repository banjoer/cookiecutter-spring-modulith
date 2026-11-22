@org.springframework.modulith.ApplicationModule(
        displayName = "Messaging Infrastructure",
        allowedDependencies = {"identity :: events", "notifications :: events"})
package {{ cookiecutter.package_name }}.messaging;
