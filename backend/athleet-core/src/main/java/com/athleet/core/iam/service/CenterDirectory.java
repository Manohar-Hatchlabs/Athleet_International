package com.athleet.core.iam.service;

import java.util.UUID;

/** Implemented by the academy module so core can validate center references without depending on it. */
public interface CenterDirectory {
    boolean existsInOrganization(UUID centerId, UUID organizationId);
}
