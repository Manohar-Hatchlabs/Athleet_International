package com.athleet.academy.attendance.domain;

public enum SessionStatus {
    DRAFT, SUBMITTED, APPROVED, REJECTED, RESUBMITTED;

    /** Marks can be changed only while the coach still owns the session. */
    public boolean editable() {
        return this == DRAFT || this == REJECTED;
    }

    /** Waiting for a reviewer. */
    public boolean pending() {
        return this == SUBMITTED || this == RESUBMITTED;
    }
}
