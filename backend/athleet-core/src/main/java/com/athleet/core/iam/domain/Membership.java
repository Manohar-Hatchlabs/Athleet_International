package com.athleet.core.iam.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/** A user's staff assignment in one organization: role, and the center for CENTER-type roles. */
@Entity
@Table(name = "membership")
public class Membership extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "center_id")
    private UUID centerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    protected Membership() {
    }

    public Membership(UUID userId, UUID organizationId, UUID roleId, UUID centerId) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.roleId = roleId;
        this.centerId = centerId;
    }

    public UUID getUserId() { return userId; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public UUID getCenterId() { return centerId; }
    public void setCenterId(UUID centerId) { this.centerId = centerId; }
    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }
}
