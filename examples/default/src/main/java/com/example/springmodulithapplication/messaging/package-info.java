@org.springframework.modulith.ApplicationModule(
        displayName = "Messaging Infrastructure",
        allowedDependencies = {"identity :: events", "notifications :: events"})
package com.example.springmodulithapplication.messaging;
