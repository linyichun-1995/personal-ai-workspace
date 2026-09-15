package com.example.workspace.dashboard.controller;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.dashboard.application.DashboardService;
import com.example.workspace.dashboard.dto.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardResponse get(CurrentUser currentUser) {
        return dashboardService.get(currentUser);
    }
}
