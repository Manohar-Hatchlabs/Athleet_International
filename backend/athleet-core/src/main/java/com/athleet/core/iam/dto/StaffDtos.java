package com.athleet.core.iam.dto;

import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.domain.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public final class StaffDtos {

    private StaffDtos() {
    }

    public static final String PERSON_NAME = "^[\\p{L} .'-]+$";

    public record StaffRequest(
            @NotNull UUID roleId,
            UUID centerId,
            @NotBlank @Email @Size(max = 254) String email,
            @Pattern(regexp = "^\\+?\\d{1,4}$", message = "invalid country code") String countryCode,
            @Pattern(regexp = "^\\d{6,15}$", message = "must be 6-15 digits") String mobile,
            @NotBlank @Size(max = 60) @Pattern(regexp = PERSON_NAME, message = "contains invalid characters") String firstName,
            @Size(max = 60) @Pattern(regexp = "^[\\p{L} .'-]*$", message = "contains invalid characters") String lastName,
            @PastOrPresent LocalDate dateOfJoining,
            @Size(max = 300) String qualifications,
            @Size(max = 1000) String profile) {
    }

    public record StaffResponse(UUID membershipId, UUID userId, String firstName, String lastName, String email,
                                String countryCode, String mobile, UUID roleId, String roleName, RoleType roleType,
                                UUID centerId, MembershipStatus status, LocalDate dateOfJoining,
                                String qualifications, String profile, boolean passwordSet) {
    }
}
