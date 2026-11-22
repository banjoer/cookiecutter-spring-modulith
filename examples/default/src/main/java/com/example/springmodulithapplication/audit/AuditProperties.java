package com.example.springmodulithapplication.audit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the optional audit module.
 */
@Validated
@ConfigurationProperties(prefix = "app.modules.audit")
public class AuditProperties {

    /**
     * Whether the audit module and its event listeners are active.
     */
    private boolean enabled = true;

    /**
     * Upper bound applied to audit queries.
     */
    @Min(1)
    @Max(1000)
    private int maxPageSize = 100;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }
}
