package com.example.workspace.workspace.controller;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.infrastructure.openapi.AuthenticatedApi;
import com.example.workspace.workspace.application.WorkspaceAccess;
import com.example.workspace.workspace.application.WorkspaceAccessService;
import com.example.workspace.workspace.application.WorkspaceService;
import com.example.workspace.workspace.domain.Workspace;
import com.example.workspace.workspace.dto.UpdateWorkspaceRequest;
import com.example.workspace.workspace.dto.WorkspaceDetailResponse;
import com.example.workspace.workspace.dto.WorkspaceListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces")
@AuthenticatedApi
public class WorkspaceController {

    private final WorkspaceAccessService workspaceAccessService;
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceAccessService workspaceAccessService, WorkspaceService workspaceService) {
        this.workspaceAccessService = workspaceAccessService;
        this.workspaceService = workspaceService;
    }

    @GetMapping
    @Operation(summary = "工作空间列表", description = "返回当前用户可访问的工作空间。V0.1 注册后会有一个默认个人空间。")
    @ApiResponse(responseCode = "200", description = "工作空间列表", content = @Content(schema = @Schema(implementation = WorkspaceListResponse.class)))
    public WorkspaceListResponse list(CurrentUser currentUser) {
        return new WorkspaceListResponse(
                workspaceAccessService.listAccessible(currentUser.userId()).stream()
                        .map(WorkspaceDetailResponse::from)
                        .toList()
        );
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "工作空间详情", description = "非成员返回 404，避免泄露空间是否存在。")
    @ApiResponse(responseCode = "200", description = "工作空间详情", content = @Content(schema = @Schema(implementation = WorkspaceDetailResponse.class)))
    @ApiResponse(responseCode = "404", description = "工作空间不存在或当前用户不是成员")
    public WorkspaceDetailResponse get(
            CurrentUser currentUser,
            @Parameter(description = "工作空间 ID", required = true) @PathVariable UUID workspaceId
    ) {
        return WorkspaceDetailResponse.from(workspaceAccessService.requireMember(currentUser.userId(), workspaceId));
    }

    @PatchMapping("/{workspaceId}")
    @Operation(summary = "更新工作空间", description = "仅 OWNER 可改名称、时区和一周起始日。需要携带当前 `version`。")
    @ApiResponse(responseCode = "200", description = "更新后的工作空间", content = @Content(schema = @Schema(implementation = WorkspaceDetailResponse.class)))
    @ApiResponse(responseCode = "403", description = "当前用户不是 OWNER")
    @ApiResponse(responseCode = "404", description = "工作空间不存在或当前用户不是成员")
    @ApiResponse(responseCode = "409", description = "工作空间已被其他请求更新")
    @ApiResponse(responseCode = "422", description = "时区无效")
    public WorkspaceDetailResponse update(
            CurrentUser currentUser,
            @Parameter(description = "工作空间 ID", required = true) @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        WorkspaceAccess access = workspaceAccessService.requireOwner(currentUser.userId(), workspaceId);
        Workspace updated = workspaceService.updateSettings(access, request);
        return WorkspaceDetailResponse.from(new WorkspaceAccess(updated, access.member()));
    }
}
