package com.athleet.core.common.security;

import java.util.UUID;

/** The authenticated principal, built from a verified access token. */
public record AuthUser(UUID id, boolean platformAdmin) {
}
