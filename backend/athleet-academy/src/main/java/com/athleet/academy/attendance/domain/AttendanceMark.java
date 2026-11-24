package com.athleet.academy.attendance.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/** One student's mark in a session. A null status means "not marked yet". */
@Entity
@Table(name = "attendance_mark")
public class AttendanceMark extends BaseEntity {

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    private MarkStatus status;

    private String remark;

    protected AttendanceMark() {
    }

    public AttendanceMark(UUID sessionId, UUID studentId) {
        this.sessionId = sessionId;
        this.studentId = studentId;
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getStudentId() { return studentId; }
    public MarkStatus getStatus() { return status; }
    public void setStatus(MarkStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
