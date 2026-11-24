package com.athleet.academy.attendance.domain;

import com.athleet.core.common.error.InvalidTransitionException;

import java.util.Map;

import static com.athleet.academy.attendance.domain.SessionAction.*;
import static com.athleet.academy.attendance.domain.SessionStatus.*;

/**
 * The only place that defines which session actions are allowed from which status.
 * <pre>
 * DRAFT       --save--> DRAFT        --submit--> SUBMITTED
 * SUBMITTED   --approve--> APPROVED  --reject--> REJECTED
 * REJECTED    --save--> REJECTED     --submit--> RESUBMITTED
 * RESUBMITTED --approve--> APPROVED  --reject--> REJECTED
 * APPROVED    --reopen--> REJECTED   (a reviewer sends it back for correction)
 * </pre>
 */
public final class SessionWorkflow {

    private static final Map<SessionStatus, Map<SessionAction, SessionStatus>> TRANSITIONS = Map.of(
            DRAFT, Map.of(SAVE, DRAFT, SUBMIT, SUBMITTED),
            SUBMITTED, Map.of(APPROVE, APPROVED, REJECT, REJECTED),
            REJECTED, Map.of(SAVE, REJECTED, SUBMIT, RESUBMITTED),
            RESUBMITTED, Map.of(APPROVE, APPROVED, REJECT, REJECTED),
            APPROVED, Map.of(REOPEN, REJECTED));

    private SessionWorkflow() {
    }

    public static SessionStatus next(SessionStatus from, SessionAction action) {
        SessionStatus to = TRANSITIONS.getOrDefault(from, Map.of()).get(action);
        if (to == null) {
            throw new InvalidTransitionException("This attendance session is " + from.name().toLowerCase()
                    + " and cannot be " + switch (action) {
                        case SAVE -> "edited";
                        case SUBMIT -> "submitted";
                        case APPROVE -> "approved";
                        case REJECT -> "rejected";
                        case REOPEN -> "reopened";
                    } + " now.");
        }
        return to;
    }
}
