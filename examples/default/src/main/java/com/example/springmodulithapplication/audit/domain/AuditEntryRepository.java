package com.example.springmodulithapplication.audit.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence port owned by the audit module.
 */
public interface AuditEntryRepository extends JpaRepository<AuditEntry, UUID> {

    List<AuditEntry> findAllByOrderByRecordedAtDesc(Pageable pageable);
}
