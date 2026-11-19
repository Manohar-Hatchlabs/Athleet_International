package com.athleet.core.iam.web;

import com.athleet.core.common.paging.PageResponse;
import com.athleet.core.iam.dto.RoleDtos.RoleRequest;
import com.athleet.core.iam.dto.RoleDtos.RoleResponse;
import com.athleet.core.iam.dto.RoleDtos.RoleSummary;
import com.athleet.core.iam.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/orgs/{orgId}/roles")
public class RoleController {

    private final RoleService roles;

    public RoleController(RoleService roles) {
        this.roles = roles;
    }

    @GetMapping
    public PageResponse<RoleSummary> list(@PathVariable UUID orgId,
                                          @RequestParam(required = false) @Size(max = 100) String search,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String sort) {
        return roles.list(orgId, search, page, size, sort);
    }

    @GetMapping("/assignable")
    public PageResponse<RoleSummary> assignable(@PathVariable UUID orgId) {
        return roles.assignable(orgId);
    }

    @GetMapping("/{roleId}")
    public RoleResponse get(@PathVariable UUID orgId, @PathVariable UUID roleId) {
        return roles.get(orgId, roleId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@PathVariable UUID orgId, @Valid @RequestBody RoleRequest req) {
        return roles.create(orgId, req);
    }

    @PutMapping("/{roleId}")
    public RoleResponse update(@PathVariable UUID orgId, @PathVariable UUID roleId, @Valid @RequestBody RoleRequest req) {
        return roles.update(orgId, roleId, req);
    }

    @DeleteMapping("/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID orgId, @PathVariable UUID roleId) {
        roles.delete(orgId, roleId);
    }
}
