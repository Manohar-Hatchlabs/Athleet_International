package com.athleet.academy.attendance.domain;

import com.athleet.core.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One shift of one staff member: an in punch and, once done, an out punch. */
@Entity
@Table(name = "staff_attendance")
public class StaffAttendance extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "center_id") private UUID centerId;
    @Column(name = "batch_id") private UUID batchId;

    /** The Asia/Kolkata calendar date of {@code inAt}. */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "in_at", nullable = false) private Instant inAt;
    @Column(name = "out_at") private Instant outAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private PunchSource source;

    @Column(name = "in_client_id") private UUID inClientId;
    @Column(name = "out_client_id") private UUID outClientId;

    @Column(name = "in_latitude") private BigDecimal inLatitude;
    @Column(name = "in_longitude") private BigDecimal inLongitude;
    @Column(name = "in_accuracy_m") private Integer inAccuracyM;
    @Column(name = "in_distance_m") private Integer inDistanceM;
    @Column(name = "in_outside_geofence") private Boolean inOutsideGeofence;

    @Column(name = "out_latitude") private BigDecimal outLatitude;
    @Column(name = "out_longitude") private BigDecimal outLongitude;
    @Column(name = "out_accuracy_m") private Integer outAccuracyM;
    @Column(name = "out_distance_m") private Integer outDistanceM;
    @Column(name = "out_outside_geofence") private Boolean outOutsideGeofence;

    @Column(name = "in_selfie_file_id") private UUID inSelfieFileId;
    @Column(name = "out_selfie_file_id") private UUID outSelfieFileId;

    @Column(name = "device_id") private String deviceId;
    @Column(name = "phone_type") private String phoneType;
    @Column(name = "tasks_accomplished") private String tasksAccomplished;

    @Column(name = "corrected_by") private UUID correctedBy;
    @Column(name = "corrected_at") private Instant correctedAt;
    @Column(name = "correction_reason") private String correctionReason;

    protected StaffAttendance() {
    }

    public StaffAttendance(UUID organizationId, UUID userId, UUID centerId, UUID batchId, LocalDate workDate,
                           Instant inAt, PunchSource source) {
        this.organizationId = organizationId;
        this.userId = userId;
        this.centerId = centerId;
        this.batchId = batchId;
        this.workDate = workDate;
        this.inAt = inAt;
        this.source = source;
    }

    public void recordIn(UUID clientId, BigDecimal lat, BigDecimal lng, Integer accuracyM, Integer distanceM,
                         Boolean outside, UUID selfieFileId, String deviceId, String phoneType) {
        this.inClientId = clientId;
        this.inLatitude = lat;
        this.inLongitude = lng;
        this.inAccuracyM = accuracyM;
        this.inDistanceM = distanceM;
        this.inOutsideGeofence = outside;
        this.inSelfieFileId = selfieFileId;
        this.deviceId = deviceId;
        this.phoneType = phoneType;
    }

    public void recordOut(Instant outAt, UUID clientId, BigDecimal lat, BigDecimal lng, Integer accuracyM,
                          Integer distanceM, Boolean outside, UUID selfieFileId, String tasks) {
        this.outAt = outAt;
        this.outClientId = clientId;
        this.outLatitude = lat;
        this.outLongitude = lng;
        this.outAccuracyM = accuracyM;
        this.outDistanceM = distanceM;
        this.outOutsideGeofence = outside;
        this.outSelfieFileId = selfieFileId;
        this.tasksAccomplished = tasks;
    }

    /** A supervisor's correction of the times; keeps who did it and why. */
    public void correct(UUID by, Instant at, String reason, LocalDate newWorkDate, Instant newIn, Instant newOut) {
        this.workDate = newWorkDate;
        this.inAt = newIn;
        this.outAt = newOut;
        this.correctedBy = by;
        this.correctedAt = at;
        this.correctionReason = reason;
    }

    public UUID getOrganizationId() { return organizationId; }
    public UUID getUserId() { return userId; }
    public UUID getCenterId() { return centerId; }
    public UUID getBatchId() { return batchId; }
    public LocalDate getWorkDate() { return workDate; }
    public Instant getInAt() { return inAt; }
    public Instant getOutAt() { return outAt; }
    public PunchSource getSource() { return source; }
    public UUID getInClientId() { return inClientId; }
    public UUID getOutClientId() { return outClientId; }
    public BigDecimal getInLatitude() { return inLatitude; }
    public BigDecimal getInLongitude() { return inLongitude; }
    public Integer getInAccuracyM() { return inAccuracyM; }
    public Integer getInDistanceM() { return inDistanceM; }
    public Boolean getInOutsideGeofence() { return inOutsideGeofence; }
    public BigDecimal getOutLatitude() { return outLatitude; }
    public BigDecimal getOutLongitude() { return outLongitude; }
    public Integer getOutAccuracyM() { return outAccuracyM; }
    public Integer getOutDistanceM() { return outDistanceM; }
    public Boolean getOutOutsideGeofence() { return outOutsideGeofence; }
    public UUID getInSelfieFileId() { return inSelfieFileId; }
    public UUID getOutSelfieFileId() { return outSelfieFileId; }
    public String getDeviceId() { return deviceId; }
    public String getPhoneType() { return phoneType; }
    public String getTasksAccomplished() { return tasksAccomplished; }
    public UUID getCorrectedBy() { return correctedBy; }
    public Instant getCorrectedAt() { return correctedAt; }
    public String getCorrectionReason() { return correctionReason; }
}
