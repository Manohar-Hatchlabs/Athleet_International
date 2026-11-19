package com.athleet.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    /** 8–72 characters (bcrypt limit) with at least one letter and one digit. */
    public static final String PASSWORD_PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$";
    public static final String PASSWORD_MESSAGE = "must be 8-72 characters with at least one letter and one digit";

    public record LoginRequest(
            @NotBlank @Size(max = 254) String identifier,
            @NotBlank @Size(max = 72) String password) {
    }

    public record TokenResponse(String accessToken, long expiresIn, MeResponse user) {
    }

    public record MeResponse(UUID id, String firstName, String lastName, String email, boolean platformAdmin) {
    }

    public record ChangePasswordRequest(
            @NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String newPassword) {
    }

    public record ForgotPasswordRequest(@NotBlank @Size(max = 254) String identifier) {
    }

    public record SetPasswordRequest(
            @NotBlank @Size(max = 100) String token,
            @NotBlank @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE) String newPassword) {
    }
}
