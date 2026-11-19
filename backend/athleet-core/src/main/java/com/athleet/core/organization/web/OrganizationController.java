package com.athleet.core.organization.web;

import com.athleet.core.organization.dto.OrganizationDtos.AccessibleOrganization;
import com.athleet.core.organization.dto.OrganizationDtos.BrandingRequest;
import com.athleet.core.organization.dto.OrganizationDtos.MyAccessResponse;
import com.athleet.core.organization.dto.OrganizationDtos.OrganizationRequest;
import com.athleet.core.organization.dto.OrganizationDtos.OrganizationResponse;
import com.athleet.core.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccessibleOrganization> accessible() {
        return service.accessible();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createPrimary(@Valid @RequestBody OrganizationRequest req) {
        return service.create(null, req);
    }

    @PostMapping("/{parentId}/affiliates")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse createAffiliate(@PathVariable UUID parentId, @Valid @RequestBody OrganizationRequest req) {
        return service.create(parentId, req);
    }

    @GetMapping("/{orgId}")
    public OrganizationResponse get(@PathVariable UUID orgId) {
        return service.get(orgId);
    }

    @GetMapping("/{orgId}/my-access")
    public MyAccessResponse myAccess(@PathVariable UUID orgId) {
        return service.myAccess(orgId);
    }

    @PutMapping("/{orgId}")
    public OrganizationResponse update(@PathVariable UUID orgId, @Valid @RequestBody OrganizationRequest req) {
        return service.update(orgId, req);
    }

    @PutMapping("/{orgId}/branding")
    public OrganizationResponse branding(@PathVariable UUID orgId, @Valid @RequestBody BrandingRequest req) {
        return service.updateBranding(orgId, req);
    }
}
