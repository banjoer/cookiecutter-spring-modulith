package com.example.springmodulithapplication.audit.api;

import java.util.List;

/**
 * Public API of the audit module.
 */
public interface AuditApi {

    /**
     * Returns the most recent entries, capped by the module's configured maximum page size.
     */
    List<AuditEntryView> recent(int limit);
}
