package com.athleet.core.iam.service;

import com.athleet.core.common.error.NotFoundException;
import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;

import java.util.Map;
import java.util.UUID;

/**
 * What the current user may do in one organization.
 *
 * @param centerId non-null when the user holds a CENTER-type role: every query must be limited to that center
 */
public record OrgAccess(UUID organizationId, UUID userId, Map<AppModule, AccessLevel> permissions, UUID centerId) {

    public boolean allCenters() {
        return centerId == null;
    }

    public boolean can(AppModule module, AccessLevel level) {
        return permissions.getOrDefault(module, AccessLevel.NONE).allows(level);
    }

    /** Throws 404 when a center-restricted user touches another center's data. */
    public void requireCenter(UUID otherCenterId) {
        if (centerId != null && !centerId.equals(otherCenterId)) {
            throw new NotFoundException("Not found.");
        }
    }
}
