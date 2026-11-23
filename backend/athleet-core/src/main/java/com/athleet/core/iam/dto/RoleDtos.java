package com.athleet.core.iam.dto;

import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.iam.domain.RoleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public final class RoleDtos {

    private RoleDtos() {
    }

    public static final String NAME_PATTERN = "^[\\p{L}\\p{N} .,'&()/-]+$";

    public record RoleRequest(
            @NotBlank @Size(max = 60) @Pattern(regexp = NAME_PATTERN, message = "contains invalid characters") String name,
            @NotNull RoleType roleType,
            Map<AppModule, AccessLevel> permissions) {
    }

    public record RoleResponse(UUID id, String name, RoleType roleType, boolean systemRole,
                               Map<AppModule, AccessLevel> permissions) {
    }

    public record RoleSummary(UUID id, String name, RoleType roleType, boolean systemRole) {
    }
}
