package com.athleet.core.common.security;

/** Masks personal data before it reaches a log line. */
public final class Masking {

    private Masking() {
    }

    public static String phone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 2) + "x".repeat(phone.length() - 4) + phone.substring(phone.length() - 2);
    }

    public static String email(String email) {
        if (email == null) return "****";
        int at = email.indexOf('@');
        if (at <= 1) return "****";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
