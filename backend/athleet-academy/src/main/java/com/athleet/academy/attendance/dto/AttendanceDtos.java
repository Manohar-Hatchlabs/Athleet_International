package com.athleet.academy.attendance.dto;

import com.athleet.academy.attendance.domain.MarkStatus;
import com.athleet.academy.attendance.domain.PunchSource;
import com.athleet.academy.attendance.domain.SessionStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    public enum TrackerType { STAFF, STUDENT }

    /** Status filter of the tracker: the person's own status, not an attendance status. */
    public enum PersonStatus { ACTIVE, DEREGISTERED }

    // ---- configuration ----

    public record ConfigRequest(
            boolean selfieRequired,
            boolean tasksOnOut,
            boolean showDeviceInfo,
            boolean allowDateOverride,
            @Min(0) @Max(31) int maxBackDays,
            @Min(0) @Max(7) int maxForwardDays) {
    }

    public record ConfigResponse(boolean selfieRequired, boolean tasksOnOut, boolean showDeviceInfo,
                                 boolean allowDateOverride, int maxBackDays, int maxForwardDays) {
    }

    // ---- student sessions ----

    public record MarkRequest(@NotNull UUID studentId, MarkStatus status, @Size(max = 200) String remark) {
    }

    /** {@code version} is null when the session does not exist yet, otherwise the version last read. */
    public record SaveSessionRequest(Long version, @NotNull @Size(max = 200) List<@Valid MarkRequest> marks) {
    }

    public record TransitionRequest(@NotNull Long version, @Size(min = 3, max = 500) String reason) {
    }

    public record MarkResponse(UUID studentId, long studentNo, String studentName, MarkStatus status, String remark) {
    }

    public record SessionDetail(UUID id, Long version, UUID centerId, UUID batchId, String batchName, LocalDate date,
                                SessionStatus status, String reviewReason, boolean editable, boolean canSubmit,
                                boolean canReview, List<MarkResponse> marks) {
    }

    public record SessionSummary(UUID id, Long version, UUID centerId, String centerName, UUID batchId, String batchName,
                                 LocalDate date, SessionStatus status, long present, long absent, long unmarked) {
    }

    /** A batch that trains on a date, with its session when one exists. */
    public record SessionBatch(UUID batchId, String batchName, UUID centerId, String centerName, UUID sessionId,
                               SessionStatus status, long present, long absent, long total) {
    }

    // ---- staff punches ----

    public record PunchInRequest(
            @NotNull UUID clientId,
            UUID centerId,
            UUID batchId,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Min(0) @Max(100000) Integer accuracyM,
            @Size(max = 100) String deviceId,
            @Size(max = 60) String phoneType,
            UUID selfieFileId,
            Instant capturedAt) {
    }

    public record PunchOutRequest(
            @NotNull UUID clientId,
            @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
            @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
            @Min(0) @Max(100000) Integer accuracyM,
            UUID selfieFileId,
            @Size(max = 1000) String tasksAccomplished,
            Instant capturedAt) {
    }

    /** A supervisor adds a shift for a staff member who forgot to punch. */
    public record AdminShiftRequest(
            @NotNull UUID userId,
            UUID centerId,
            @NotNull Instant inAt,
            Instant outAt,
            @NotBlank @Size(min = 3, max = 300) String reason) {
    }

    public record AdminCorrectionRequest(
            @NotNull Long version,
            @NotNull Instant inAt,
            Instant outAt,
            @NotBlank @Size(min = 3, max = 300) String reason) {
    }

    public record ShiftResponse(UUID id, Long version, UUID userId, String staffName, UUID centerId, String centerName,
                                UUID batchId, String batchName, LocalDate workDate, Instant inAt, Instant outAt,
                                PunchSource source, Integer inDistanceM, Boolean inOutsideGeofence, Integer outDistanceM,
                                Boolean outOutsideGeofence, boolean hasInSelfie, boolean hasOutSelfie, String tasks,
                                String deviceId, String phoneType, boolean corrected) {
    }

    /** One shift with short-lived selfie links. */
    public record ShiftDetail(ShiftResponse shift, String inSelfieUrl, String outSelfieUrl, String correctionReason) {
    }

    public record MyDay(LocalDate date, ConfigResponse config, ShiftResponse openShift, List<ShiftResponse> shifts) {
    }

    // ---- reports ----

    public record LiveStaffRow(UUID userId, String name, String status, Instant inAt, Instant outAt, String batchName,
                               Boolean outsideGeofence, String deviceId, String phoneType) {
    }

    public record LiveSessionRow(UUID batchId, String batchName, List<String> coaches, SessionStatus status, long present,
                                 long absent, long total) {
    }

    public record LiveGroup(UUID id, String name, List<LiveStaffRow> staff, List<LiveSessionRow> sessions) {
    }

    public record LiveAttendance(LocalDate date, String groupedBy, List<LiveGroup> groups) {
    }

    /** One cell of the tracker grid. {@code code}: P / A for students; IN / OUT / OPEN for staff. */
    public record TrackerCell(LocalDate date, String code, boolean pending, Instant inAt, Instant outAt,
                              Boolean outsideGeofence) {
    }

    public record TrackerRow(UUID id, String name, String subtitle, List<TrackerCell> cells, long present, long absent,
                             int percent) {
    }

    public record TrackerGrid(String type, LocalDate from, LocalDate to, List<LocalDate> days, List<TrackerRow> rows,
                              int page, int size, long total) {
    }

    public record StudentDay(LocalDate date, String batchName, MarkStatus status, SessionStatus sessionStatus) {
    }

    public record StudentMonth(UUID studentId, String studentName, String month, long present, long absent, int percent,
                               List<StudentDay> days) {
    }
}
