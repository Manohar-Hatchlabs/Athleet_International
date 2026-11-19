package com.athleet.core.common.security;

import java.util.UUID;

/** Lets the token filter reject users who were deactivated after their token was issued. */
public interface UserStatusChecker {
    boolean isActive(UUID userId);
}
