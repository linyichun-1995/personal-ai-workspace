package com.example.workspace.workspace.application;

import static com.example.workspace.infrastructure.database.jooq.Tables.WORKSPACES;

import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.workspace.domain.Workspace;
import org.springframework.stereotype.Component;
import org.jooq.DSLContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CurrentWorkspaceResolver {

    private final WorkspaceService workspaceService;
    private final WorkspaceAccessService workspaceAccessService;
    private final DSLContext db;

    public CurrentWorkspaceResolver(
            WorkspaceService workspaceService,
            WorkspaceAccessService workspaceAccessService,
            DSLContext db
    ) {
        this.workspaceService = workspaceService;
        this.workspaceAccessService = workspaceAccessService;
        this.db = db;
    }

    public WorkspaceAccess require(CurrentUser currentUser) {
        Workspace workspace = workspaceService.requireDefaultForUser(currentUser.userId());
        WorkspaceAccess access = workspaceAccessService.requireMember(currentUser.userId(), workspace.id());
        // Serialize short write transactions within a workspace. Reads, uploads and parsing do not
        // hold this lock. All lifecycle checks and quota/tag mutations share the same lock order.
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && !TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            db.select(WORKSPACES.ID).from(WORKSPACES).where(WORKSPACES.ID.eq(workspace.id())).forUpdate().fetch();
        }
        return access;
    }
}
