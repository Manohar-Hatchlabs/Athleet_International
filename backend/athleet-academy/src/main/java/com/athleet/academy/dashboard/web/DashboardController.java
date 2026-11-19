package com.athleet.academy.dashboard.web;

import com.athleet.academy.dashboard.dto.DashboardDtos.GeneralDashboard;
import com.athleet.academy.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orgs/{orgId}/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/general")
    public GeneralDashboard general(@PathVariable UUID orgId,
                                    @RequestParam(defaultValue = "false") boolean includeAffiliates) {
        return dashboard.general(orgId, includeAffiliates);
    }
}
