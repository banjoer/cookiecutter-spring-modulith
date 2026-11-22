@org.springframework.modulith.ApplicationModule(
        displayName = "Audit",
        allowedDependencies = {"identity :: events", "notifications :: events"})
package com.example.springmodulithapplication.audit;
