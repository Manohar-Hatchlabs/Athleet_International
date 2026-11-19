package com.athleet.core.iam.domain;

public enum AccessLevel {
    NONE, VIEW, MANAGE;

    public boolean allows(AccessLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
