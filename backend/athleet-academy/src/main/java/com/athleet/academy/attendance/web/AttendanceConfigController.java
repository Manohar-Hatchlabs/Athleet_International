package com.athleet.academy.attendance.web;

import com.athleet.academy.attendance.dto.AttendanceDtos.ConfigRequest;
import com.athleet.academy.attendance.dto.AttendanceDtos.ConfigResponse;
import com.athleet.academy.attendance.service.AttendanceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orgs/{orgId}/attendance/config")
@Tag(name = "Attendance")
public class AttendanceConfigController {

    private final AttendanceConfigService service;

    public AttendanceConfigController(AttendanceConfigService service) {
        this.service = service;
    }

    @Operation(summary = "Attendance Configuration (readable by any staff member)")
    @GetMapping
    public ConfigResponse get(@PathVariable UUID orgId) {
        return service.get(orgId);
    }

    @Operation(summary = "Save Attendance Configuration (needs Settings: manage)")
    @PutMapping
    public ConfigResponse update(@PathVariable UUID orgId, @Valid @RequestBody ConfigRequest req) {
        return service.update(orgId, req);
    }
}
