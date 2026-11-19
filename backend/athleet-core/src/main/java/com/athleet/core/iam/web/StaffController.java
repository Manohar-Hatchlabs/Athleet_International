package com.athleet.core.iam.web;

import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.iam.domain.MembershipStatus;
import com.athleet.core.iam.dto.StaffDtos.StaffRequest;
import com.athleet.core.iam.dto.StaffDtos.StaffResponse;
import com.athleet.core.iam.service.StaffService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/orgs/{orgId}/staff")
public class StaffController {

    private final StaffService staff;

    public StaffController(StaffService staff) {
        this.staff = staff;
    }

    @GetMapping
    public PageResponse<StaffResponse> list(@PathVariable UUID orgId,
                                            @RequestParam(required = false) @Size(max = 100) String search,
                                            @RequestParam(required = false) MembershipStatus status,
                                            @RequestParam(required = false) UUID centerId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return staff.list(orgId, search, status, centerId, page, size);
    }

    @GetMapping("/{membershipId}")
    public StaffResponse get(@PathVariable UUID orgId, @PathVariable UUID membershipId) {
        return staff.get(orgId, membershipId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse create(@PathVariable UUID orgId, @Valid @RequestBody StaffRequest req) {
        return staff.create(orgId, req);
    }

    @PutMapping("/{membershipId}")
    public StaffResponse update(@PathVariable UUID orgId, @PathVariable UUID membershipId, @Valid @RequestBody StaffRequest req) {
        return staff.update(orgId, membershipId, req);
    }

    @PostMapping("/{membershipId}/deregister")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deregister(@PathVariable UUID orgId, @PathVariable UUID membershipId) {
        staff.deregister(orgId, membershipId);
    }
}
