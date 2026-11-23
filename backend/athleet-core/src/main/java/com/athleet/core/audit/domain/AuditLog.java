package com.athleet.core.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Append-only record of who changed what. Details must not contain personal data beyond ids. */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(nullable = false)
    private String action;

    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(UUID organizationId, UUID actorUserId, String entityType, UUID entityId, String action, String details) {
        this.organizationId = organizationId;
        this.actorUserId = actorUserId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.details = details == null ? null : details.substring(0, Math.min(details.length(), 2000));
    }
}
