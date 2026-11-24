package com.athleet.academy.attendance.service;

import com.athleet.academy.attendance.domain.AttendanceSessionApproved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Placeholder for absence alerts to parents. They are not sent in Phase 1; this is where they attach once a real
 * {@code MessageSender} exists. Runs after the approving transaction commits and logs ids and counts only.
 */
@Component
class AbsenceAlertListener {

    private static final Logger log = LoggerFactory.getLogger(AbsenceAlertListener.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onApproved(AttendanceSessionApproved event) {
        log.info("Attendance approved: session={} batch={} date={} absent={} (absence alerts are not enabled yet)",
                event.sessionId(), event.batchId(), event.sessionDate(), event.absentStudentIds().size());
    }
}
