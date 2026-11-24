package com.athleet.academy.attendance.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Published after a session is approved. Listeners run after the transaction commits (for example absence alerts later).
 * A reopen followed by a new approval publishes it again, so listeners must be idempotent.
 */
public record AttendanceSessionApproved(UUID organizationId, UUID centerId, UUID batchId, UUID sessionId,
                                        LocalDate sessionDate, UUID approvedBy, List<UUID> absentStudentIds) {
}
