package com.athleet.academy.center.web;

import com.athleet.academy.center.dto.CenterDtos.BatchRequest;
import com.athleet.academy.center.dto.CenterDtos.BatchResponse;
import com.athleet.academy.center.dto.CenterDtos.CenterRequest;
import com.athleet.academy.center.dto.CenterDtos.CenterResponse;
import com.athleet.academy.center.dto.CenterDtos.CenterSummary;
import com.athleet.academy.center.dto.CenterDtos.Option;
import com.athleet.academy.center.service.BatchService;
import com.athleet.academy.center.service.CenterService;
import com.athleet.core.common.paging.PageResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/orgs/{orgId}")
public class CenterController {

    private final CenterService centers;
    private final BatchService batches;

    public CenterController(CenterService centers, BatchService batches) {
        this.centers = centers;
        this.batches = batches;
    }

    @GetMapping("/centers")
    public PageResponse<CenterSummary> list(@PathVariable UUID orgId,
                                            @RequestParam(required = false) @Size(max = 100) String search,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) @Size(max = 30) String sort) {
        return centers.list(orgId, search, page, size, sort);
    }

    @GetMapping("/centers/options")
    public List<Option> options(@PathVariable UUID orgId) {
        return centers.options(orgId);
    }

    @GetMapping("/centers/{centerId}")
    public CenterResponse get(@PathVariable UUID orgId, @PathVariable UUID centerId) {
        return centers.get(orgId, centerId);
    }

    @PostMapping("/centers")
    @ResponseStatus(HttpStatus.CREATED)
    public CenterResponse create(@PathVariable UUID orgId, @Valid @RequestBody CenterRequest req) {
        return centers.create(orgId, req);
    }

    @PutMapping("/centers/{centerId}")
    public CenterResponse update(@PathVariable UUID orgId, @PathVariable UUID centerId, @Valid @RequestBody CenterRequest req) {
        return centers.update(orgId, centerId, req);
    }

    @DeleteMapping("/centers/{centerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID orgId, @PathVariable UUID centerId) {
        centers.delete(orgId, centerId);
    }

    @GetMapping("/centers/{centerId}/batches")
    public List<BatchResponse> batches(@PathVariable UUID orgId, @PathVariable UUID centerId) {
        return batches.list(orgId, centerId);
    }

    @GetMapping("/centers/{centerId}/batches/options")
    public List<Option> batchOptions(@PathVariable UUID orgId, @PathVariable UUID centerId) {
        return batches.options(orgId, centerId);
    }

    @PostMapping("/centers/{centerId}/batches")
    @ResponseStatus(HttpStatus.CREATED)
    public BatchResponse createBatch(@PathVariable UUID orgId, @PathVariable UUID centerId, @Valid @RequestBody BatchRequest req) {
        return batches.create(orgId, centerId, req);
    }

    @PutMapping("/batches/{batchId}")
    public BatchResponse updateBatch(@PathVariable UUID orgId, @PathVariable UUID batchId, @Valid @RequestBody BatchRequest req) {
        return batches.update(orgId, batchId, req);
    }

    @DeleteMapping("/batches/{batchId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBatch(@PathVariable UUID orgId, @PathVariable UUID batchId) {
        batches.delete(orgId, batchId);
    }
}
