package com.athleet.core.audit.service;

import com.athleet.core.audit.domain.AuditLog;
import com.athleet.core.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Written inside the caller's transaction so the audit record commits (or rolls back) with the change. */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID organizationId, UUID actorUserId, String entityType, UUID entityId, String action, String details) {
        repository.save(new AuditLog(organizationId, actorUserId, entityType, entityId, action, details));
    }
}
