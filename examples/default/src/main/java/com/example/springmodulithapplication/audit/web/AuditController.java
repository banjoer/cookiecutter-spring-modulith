package com.example.springmodulithapplication.audit.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.springmodulithapplication.audit.api.AuditApi;
import com.example.springmodulithapplication.audit.api.AuditEntryView;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditApi audit;

    public AuditController(AuditApi audit) {
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<AuditEntryView> recent(@RequestParam(defaultValue = "20") int limit) {
        return audit.recent(limit);
    }
}
