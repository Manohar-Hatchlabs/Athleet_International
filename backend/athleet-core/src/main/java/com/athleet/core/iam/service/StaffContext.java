package com.athleet.core.iam.service;

import com.athleet.core.iam.domain.RoleType;

import java.util.UUID;

/**
 * The caller's own staff assignment in one organization (no inheritance from a parent organization).
 *
 * @param centerId the center of a CENTER-type role; null for head-office roles
 */
public record StaffContext(UUID userId, UUID organizationId, RoleType roleType, UUID centerId) {

    public boolean centerRole() {
        return roleType == RoleType.CENTER;
    }
}
