package com.athleet.core.common.error;

/** Also used when a record exists but lies outside the caller's scope, so IDs cannot be probed. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
