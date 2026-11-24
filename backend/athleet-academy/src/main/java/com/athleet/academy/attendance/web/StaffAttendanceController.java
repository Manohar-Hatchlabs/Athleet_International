package com.athleet.academy.attendance.web;

import com.athleet.academy.attendance.dto.AttendanceDtos.AdminCorrectionRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.AdminShiftRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.MyDay;
import com.athleet.academy.attendance.dto.AttendanceDtos.PunchInRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.PunchOutRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.ShiftDetail;
import com.athleet.academy.attendance.dto.AttendanceDtos.ShiftResponse;
import com.athleet.academy.attendance.service.StaffAttendanceService;
import com.athleet.core.common.paging.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/orgs/{orgId}/attendance/staff")
@Tag(name = "Attendance")
public class StaffAttendanceController {

    private final StaffAttendanceService service;

    public StaffAttendanceController(StaffAttendanceService service) {
        this.service = service;
    }

    @Operation(summary = "My shifts today, my open shift, and the attendance rules")
    @GetMapping("/me/today")
    public MyDay today(@PathVariable UUID orgId) {
        return service.today(orgId);
    }

    @Operation(summary = "My shifts in a date range (at most 62 days)")
    @GetMapping("/me")
    public List<ShiftResponse> mine(@PathVariable UUID orgId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.mine(orgId, from, to);
    }

    @Operation(summary = "Punch in. Upload the selfie first (POST /files) when the academy requires one. clientId makes retries safe.")
    @PostMapping("/me/punch-in")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse punchIn(@PathVariable UUID orgId, @Valid @RequestBody PunchInRequest req) {
        return service.punchIn(orgId, req);
    }

    @Operation(summary = "Punch out of the open shift. clientId makes retries safe.")
    @PostMapping("/me/punch-out")
    public ShiftResponse punchOut(@PathVariable UUID orgId, @Valid @RequestBody PunchOutRequest req) {
        return service.punchOut(orgId, req);
    }

    @Operation(summary = "In/Out list for staff in scope")
    @GetMapping
    public PageResponse<ShiftResponse> list(@PathVariable UUID orgId,
                                            @RequestParam(required = false) UUID centerId,
                                            @RequestParam(required = false) UUID userId,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) @Size(max = 30) String sort) {
        return service.list(orgId, centerId, userId, from, to, page, size, sort);
    }

    @Operation(summary = "One shift with short-lived selfie links")
    @GetMapping("/{shiftId}")
    public ShiftDetail detail(@PathVariable UUID orgId, @PathVariable UUID shiftId) {
        return service.detail(orgId, shiftId);
    }

    @Operation(summary = "Add a shift for a staff member (needs Attendance approval: manage; reason required)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftResponse add(@PathVariable UUID orgId, @Valid @RequestBody AdminShiftRequest req) {
        return service.addShift(orgId, req);
    }

    @Operation(summary = "Correct a shift's times (needs Attendance approval: manage; reason required)")
    @PutMapping("/{shiftId}")
    public ShiftResponse correct(@PathVariable UUID orgId, @PathVariable UUID shiftId, @Valid @RequestBody AdminCorrectionRequest req) {
        return service.correct(orgId, shiftId, req);
    }
}
