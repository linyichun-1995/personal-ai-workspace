package com.example.workspace.dashboard.controller;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.dashboard.application.DashboardService;
import com.example.workspace.dashboard.dto.DashboardResponse;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
@AuthenticatedApi
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "工作台", description = "聚合当前 Workspace 的概览、今日/即将到期任务、活跃项目和最近笔记。`aiConversations` 在 V0.1 固定为 0。")
    @ApiResponse(responseCode = "200", description = "工作台数据", content = @Content(schema = @Schema(implementation = DashboardResponse.class)))
    public DashboardResponse get(CurrentUser currentUser) {
        return dashboardService.get(currentUser);
    }
}
