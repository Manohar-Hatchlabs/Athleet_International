package com.athleet.academy.attendance.web;

import com.athleet.academy.attendance.domain.SessionStatus;
import com.athleet.academy.attendance.dto.AttendanceDtos.SaveSessionRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.SessionBatch;
import com.athleet.academy.attendance.dto.AttendanceDtos.SessionDetail;
import com.athleet.academy.attendance.dto.AttendanceDtos.SessionSummary;
import com.athleet.academy.attendance.dto.AttendanceDtos.TransitionRequest;
import com.athleet.academy.attendance.service.SessionAttendanceService;
import com.athleet.core.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/orgs/{orgId}/attendance")
@Tag(name = "Attendance")
public class SessionAttendanceController {

    private final SessionAttendanceService service;

    public SessionAttendanceController(SessionAttendanceService service) {
        this.service = service;
    }

    @Operation(summary = "Batches that train on a date, with their session status. Coaches see only the batches they coach.")
    @GetMapping("/session-batches")
    public List<SessionBatch> sessionBatches(@PathVariable UUID orgId,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                             @RequestParam(required = false) UUID centerId) {
        return service.sessionBatches(orgId, date, centerId);
    }

    @Operation(summary = "Approval queue and history (filter status=SUBMITTED,RESUBMITTED for the queue)")
    @GetMapping("/sessions")
    public PageResponse<SessionSummary> list(@PathVariable UUID orgId,
                                             @RequestParam(required = false) UUID centerId,
                                             @RequestParam(required = false) UUID batchId,
                                             @RequestParam(required = false) Set<SessionStatus> status,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             @RequestParam(required = false) @Size(max = 30) String sort) {
        return service.list(orgId, centerId, batchId, status, from, to, page, size, sort);
    }

    @Operation(summary = "A batch's session and roster for a date (status is absent before the first save)")
    @GetMapping("/batches/{batchId}/sessions/{date}")
    public SessionDetail get(@PathVariable UUID orgId, @PathVariable UUID batchId,
                             @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.get(orgId, batchId, date);
    }

    @Operation(summary = "Save marks as a draft (creates the session on first save). Send the version you last read.")
    @PutMapping("/batches/{batchId}/sessions/{date}")
    public SessionDetail save(@PathVariable UUID orgId, @PathVariable UUID batchId,
                              @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              @Valid @RequestBody SaveSessionRequest req) {
        return service.save(orgId, batchId, date, req);
    }

    @Operation(summary = "Submit for approval (every student must be marked)")
    @PostMapping("/sessions/{sessionId}/submit")
    public SessionDetail submit(@PathVariable UUID orgId, @PathVariable UUID sessionId, @Valid @RequestBody TransitionRequest req) {
        return service.submit(orgId, sessionId, req);
    }

    @Operation(summary = "Approve a submitted session (needs Attendance approval: manage)")
    @PostMapping("/sessions/{sessionId}/approve")
    public SessionDetail approve(@PathVariable UUID orgId, @PathVariable UUID sessionId, @Valid @RequestBody TransitionRequest req) {
        return service.approve(orgId, sessionId, req);
    }

    @Operation(summary = "Reject a submitted session; a reason is required")
    @PostMapping("/sessions/{sessionId}/reject")
    public SessionDetail reject(@PathVariable UUID orgId, @PathVariable UUID sessionId, @Valid @RequestBody TransitionRequest req) {
        return service.reject(orgId, sessionId, req);
    }

    @Operation(summary = "Send an approved session back for correction; a reason is required")
    @PostMapping("/sessions/{sessionId}/reopen")
    public SessionDetail reopen(@PathVariable UUID orgId, @PathVariable UUID sessionId, @Valid @RequestBody TransitionRequest req) {
        return service.reopen(orgId, sessionId, req);
    }
}
