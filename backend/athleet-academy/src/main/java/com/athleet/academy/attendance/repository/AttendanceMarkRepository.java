package com.athleet.academy.attendance.repository;

import com.athleet.academy.attendance.domain.AttendanceMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttendanceMarkRepository extends JpaRepository<AttendanceMark, UUID> {

    List<AttendanceMark> findBySessionId(UUID sessionId);

    /** [sessionId, markStatus (null = not marked), count] */
    @Query("select m.sessionId, m.status, count(m) from AttendanceMark m where m.sessionId in :sessionIds group by m.sessionId, m.status")
    List<Object[]> countsBySession(Collection<UUID> sessionIds);

    /** [studentId, sessionDate, markStatus, sessionStatus, batchId] for students in a date range, always scoped by organization. */
    @Query("""
            select m.studentId, s.sessionDate, m.status, s.status, s.batchId
            from AttendanceMark m, AttendanceSession s
            where s.id = m.sessionId and s.organizationId = :orgId and m.studentId in :studentIds
              and s.sessionDate >= :from and s.sessionDate <= :to
            """)
    List<Object[]> forStudents(UUID orgId, Collection<UUID> studentIds, LocalDate from, LocalDate to);

    /** [markStatus, sessionDate] of absent students in a session (for the approval event). */
    @Query("select m.studentId from AttendanceMark m where m.sessionId = :sessionId and m.status = com.athleet.academy.attendance.domain.MarkStatus.ABSENT")
    List<UUID> absentStudentIds(UUID sessionId);
}
