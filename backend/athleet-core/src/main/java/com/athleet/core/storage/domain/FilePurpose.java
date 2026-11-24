package com.athleet.core.storage.domain;

/** Why a file was uploaded. Each purpose gets its own key prefix; later ones (logo, post media) are added here. */
public enum FilePurpose {
    ATTENDANCE_SELFIE("attendance-selfie");

    private final String keyPrefix;

    FilePurpose(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String keyPrefix() {
        return keyPrefix;
    }
}
