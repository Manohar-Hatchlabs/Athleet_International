package com.athleet.academy.attendance.web;

import com.athleet.academy.attendance.dto.AttendanceDtos.LiveAttendance;
import com.athleet.academy.attendance.dto.AttendanceDtos.PersonStatus;
import com.athleet.academy.attendance.dto.AttendanceDtos.StudentMonth;
import com.athleet.academy.attendance.dto.AttendanceDtos.TrackerGrid;
import com.athleet.academy.attendance.dto.AttendanceDtos.TrackerType;
import com.athleet.academy.attendance.service.AttendanceReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/orgs/{orgId}/attendance")
@Tag(name = "Attendance")
public class AttendanceReportController {

    private final AttendanceReportService service;

    public AttendanceReportController(AttendanceReportService service) {
        this.service = service;
    }

    @Operation(summary = "Live attendance today: staff in/out and batch sessions, per academy or class")
    @GetMapping("/live")
    public LiveAttendance live(@PathVariable UUID orgId, @RequestParam(defaultValue = "false") boolean includeAffiliates) {
        return service.live(orgId, includeAffiliates);
    }

    @Operation(summary = "Attendance tracker grid for staff or students (at most 31 days)")
    @GetMapping("/tracker")
    public TrackerGrid tracker(@PathVariable UUID orgId,
                               @RequestParam TrackerType type,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) UUID centerId,
                               @RequestParam(required = false) UUID batchId,
                               @RequestParam(required = false) PersonStatus status,
                               @RequestParam(required = false) @Size(max = 100) String search,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "25") int size) {
        return service.tracker(orgId, type, from, to, centerId, batchId, status, search, page, size);
    }

    @Operation(summary = "One student's attendance for a month (YYYY-MM)")
    @GetMapping("/students/{studentId}")
    public StudentMonth studentMonth(@PathVariable UUID orgId, @PathVariable UUID studentId,
                                     @RequestParam(required = false) @Size(max = 7) String month) {
        return service.studentMonth(orgId, studentId, month);
    }
}
