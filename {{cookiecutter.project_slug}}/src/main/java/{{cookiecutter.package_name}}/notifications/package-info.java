@org.springframework.modulith.ApplicationModule(
        displayName = "Notifications",
        allowedDependencies = {"identity :: events", "shared :: error"})
package {{ cookiecutter.package_name }}.notifications;
