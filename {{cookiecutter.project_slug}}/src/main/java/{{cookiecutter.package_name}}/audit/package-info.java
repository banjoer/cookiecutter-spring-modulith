@org.springframework.modulith.ApplicationModule(
        displayName = "Audit",
        allowedDependencies = {"identity :: events", "notifications :: events"})
package {{ cookiecutter.package_name }}.audit;
