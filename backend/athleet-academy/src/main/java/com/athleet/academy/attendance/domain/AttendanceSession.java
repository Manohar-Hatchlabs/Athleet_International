package com.athleet.academy.attendance.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One batch on one date. The status only changes through {@link #apply}, which uses {@link SessionWorkflow}. */
@Entity
@Table(name = "attendance_session")
public class AttendanceSession extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "center_id", nullable = false, updatable = false)
    private UUID centerId;

    @Column(name = "batch_id", nullable = false, updatable = false)
    private UUID batchId;

    @Column(name = "session_date", nullable = false, updatable = false)
    private LocalDate sessionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.DRAFT;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** Last person to save marks, and when. Changes on every save, which also moves the optimistic-lock version. */
    @Column(name = "marked_by") private UUID markedBy;
    @Column(name = "marked_at") private Instant markedAt;

    @Column(name = "submitted_by") private UUID submittedBy;
    @Column(name = "submitted_at") private Instant submittedAt;
    @Column(name = "reviewed_by") private UUID reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "review_reason") private String reviewReason;

    protected AttendanceSession() {
    }

    public AttendanceSession(UUID organizationId, UUID centerId, UUID batchId, LocalDate sessionDate, UUID createdBy) {
        this.organizationId = organizationId;
        this.centerId = centerId;
        this.batchId = batchId;
        this.sessionDate = sessionDate;
        this.createdBy = createdBy;
    }

    /** Moves to the next status or throws {@code InvalidTransitionException}. */
    public void apply(SessionAction action, UUID actor, String reason, Instant now) {
        status = SessionWorkflow.next(status, action);
        switch (action) {
            case SUBMIT -> {
                submittedBy = actor;
                submittedAt = now;
                reviewedBy = null;
                reviewedAt = null;
                reviewReason = null;
            }
            case APPROVE -> {
                reviewedBy = actor;
                reviewedAt = now;
                reviewReason = null;
            }
            case REJECT, REOPEN -> {
                reviewedBy = actor;
                reviewedAt = now;
                reviewReason = reason;
            }
            case SAVE -> {
                markedBy = actor;
                markedAt = now;
            }
        }
    }

    public UUID getOrganizationId() { return organizationId; }
    public UUID getCenterId() { return centerId; }
    public UUID getBatchId() { return batchId; }
    public LocalDate getSessionDate() { return sessionDate; }
    public SessionStatus getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getMarkedBy() { return markedBy; }
    public Instant getMarkedAt() { return markedAt; }
    public UUID getSubmittedBy() { return submittedBy; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewReason() { return reviewReason; }
}
