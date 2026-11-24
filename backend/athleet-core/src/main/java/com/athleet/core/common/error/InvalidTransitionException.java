package com.athleet.core.common.error;

/** A workflow action that is not allowed from the record's current state (HTTP 422 through {@link BusinessRuleException}). */
public class InvalidTransitionException extends BusinessRuleException {

    public InvalidTransitionException(String message) {
        super("ILLEGAL_TRANSITION", message);
    }
}
