package com.athleet.core.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Error body returned to every client. Never carries stack traces, SQL or rejected values. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, List<FieldViolation> fields) {

    public record FieldViolation(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }
}
