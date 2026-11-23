package com.athleet.core.organization.dto;

import com.athleet.core.iam.domain.AccessLevel;
import com.athleet.core.iam.domain.AppModule;
import com.athleet.core.organization.domain.OrgStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public static final String TEXT = "^[\\p{L}\\p{N} .,'&()/#:-]*$";

    public record OrganizationRequest(
            @NotBlank @Size(max = 150) @Pattern(regexp = TEXT, message = "contains invalid characters") String name,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String city,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String state,
            @Size(max = 100) @Pattern(regexp = TEXT, message = "contains invalid characters") String country,
            @Size(max = 300) String address,
            @Email @Size(max = 254) String email,
            @Pattern(regexp = "^\\+?[\\d ]{6,20}$", message = "invalid phone") String phone,
            @Size(max = 60) @Pattern(regexp = TEXT, message = "contains invalid characters") String sport) {
    }

    public record BrandingRequest(
            @NotBlank @Size(max = 30) @Pattern(regexp = "^[\\p{L} ]+$", message = "letters only") String centerLabel,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "ISO currency code") String currency,
            @NotBlank @Pattern(regexp = "^(d/m/Y|m/d/Y|Y-m-d)$", message = "unsupported format") String dateFormat) {
    }

    public record OrganizationResponse(UUID id, UUID parentId, String name, String city, String state, String country,
                                       String address, String email, String phone, String sport, OrgStatus status,
                                       String centerLabel, String currency, String dateFormat) {
    }

    /** One entry in the organization selector. */
    public record AccessibleOrganization(UUID id, String name, String city, String country, boolean primary,
                                         List<AccessibleOrganization> affiliates) {
    }

    /** What the signed-in user may do in an organization; drives the admin menu. */
    public record MyAccessResponse(OrganizationResponse organization, Map<AppModule, AccessLevel> permissions,
                                   UUID centerId, int affiliateCount) {
    }
}
