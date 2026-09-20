package com.example.workspace.file.application;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;

import com.example.workspace.common.api.ErrorCode;
import com.example.workspace.common.domain.SourceAccess;
import com.example.workspace.common.domain.SourceType;
import java.util.Objects;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class FileLifecycle {
    private final DSLContext db;
    private final SourceAccess access;
    public FileLifecycle(DSLContext db,SourceAccess access) {this.db=db;this.access=access;}
    public void checkParentWrite(UUID ws,UUID project) {
        if(TransactionSynchronizationManager.isActualTransactionActive() && !TransactionSynchronizationManager.isCurrentTransactionReadOnly())access.requireProject(ws,project,true);
    }
    public void checkMove(UUID ws,SourceType type,UUID id,UUID oldProject,UUID newProject) {
        if(Objects.equals(oldProject,newProject))return;
        boolean linked = type == SourceType.TASK
                ? db.fetchExists(TASK_FILES, TASK_FILES.WORKSPACE_ID.eq(ws).and(TASK_FILES.TASK_ID.eq(id)))
                : db.fetchExists(NOTE_FILES, NOTE_FILES.WORKSPACE_ID.eq(ws).and(NOTE_FILES.NOTE_ID.eq(id)));
        if(linked)
            throw SourceAccess.conflict(ErrorCode.FILE_HAS_REFERENCES,"请先解除附件关联，再移动到其他项目");
    }
}
