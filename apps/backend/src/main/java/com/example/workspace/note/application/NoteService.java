package com.example.workspace.note.application;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.domain.SourceType;
import com.example.workspace.file.application.FileLifecycle;
import com.example.workspace.common.exception.ResourceNotFoundException;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.common.util.MarkdownSummaries;
import com.example.workspace.common.util.UuidV7;
import com.example.workspace.note.domain.Note;
import com.example.workspace.note.dto.CreateNoteRequest;
import com.example.workspace.note.dto.NoteResponse;
import com.example.workspace.note.dto.UpdateNoteRequest;
import com.example.workspace.note.repository.NoteRepository;
import com.example.workspace.project.application.ProjectService;
import com.example.workspace.workspace.application.CurrentWorkspaceResolver;
import com.example.workspace.workspace.application.WorkspaceAccess;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NoteService {

    public static final String DEFAULT_TITLE = "无标题";

    private final CurrentWorkspaceResolver currentWorkspaceResolver;
    private final NoteRepository noteRepository;
    private final ProjectService projectService;
    private final FileLifecycle fileLifecycle;

    public NoteService(
            CurrentWorkspaceResolver currentWorkspaceResolver,
            NoteRepository noteRepository,
            ProjectService projectService,
            FileLifecycle fileLifecycle
    ) {
        this.currentWorkspaceResolver = currentWorkspaceResolver;
        this.noteRepository = noteRepository;
        this.projectService = projectService;
        this.fileLifecycle = fileLifecycle;
    }

    @Transactional
    public NoteResponse create(CurrentUser currentUser, CreateNoteRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        UUID projectId = resolveProjectId(workspaceId, request.projectId());
        Instant now = Instant.now();
        String content = request.content() == null ? "" : request.content();
        Note note = new Note(
                UuidV7.next(),
                workspaceId,
                projectId,
                normalizeTitle(request.title()),
                content,
                MarkdownSummaries.from(content),
                false,
                false,
                currentUser.userId(),
                now,
                now,
                null,
                0
        );
        noteRepository.insert(note);
        return NoteResponse.from(note, true);
    }

    @Transactional(readOnly = true)
    public PageResponse<NoteResponse> list(
            CurrentUser currentUser,
            UUID projectId,
            Boolean favorite,
            Boolean archived,
            String keyword,
            PageQuery pageQuery
    ) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        if (projectId != null) {
            projectService.requireProject(workspaceId, projectId);
        }
        boolean archivedOnly = Boolean.TRUE.equals(archived);
        var notes = noteRepository.list(workspaceId, projectId, favorite, archivedOnly, keyword, pageQuery);
        long total = noteRepository.count(workspaceId, projectId, favorite, archivedOnly, keyword);
        return PageResponse.of(notes.stream().map(note -> NoteResponse.from(note, false)).toList(), pageQuery, total);
    }

    @Transactional(readOnly = true)
    public NoteResponse get(CurrentUser currentUser, UUID noteId) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        return NoteResponse.from(requireNote(access.workspace().id(), noteId), true);
    }

    @Transactional
    public NoteResponse update(CurrentUser currentUser, UUID noteId, UpdateNoteRequest request) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        UUID workspaceId = access.workspace().id();
        Note note = requireNote(workspaceId, noteId);
        UUID projectId = resolveProjectId(workspaceId, request.projectId());
        fileLifecycle.checkMove(workspaceId, SourceType.NOTE, noteId, note.projectId(), projectId);
        String content = request.content() == null ? note.content() : request.content();
        Note updated = noteRepository.update(new Note(
                note.id(),
                note.workspaceId(),
                projectId,
                request.title() == null ? note.title() : normalizeTitle(request.title()),
                content,
                MarkdownSummaries.from(content),
                request.favorite() == null ? note.favorite() : request.favorite(),
                request.archived() == null ? note.archived() : request.archived(),
                note.createdBy(),
                note.createdAt(),
                note.updatedAt(),
                note.deletedAt(),
                request.version()
        ), Instant.now());
        return NoteResponse.from(updated, true);
    }

    @Transactional
    public void delete(CurrentUser currentUser, UUID noteId) {
        WorkspaceAccess access = currentWorkspaceResolver.require(currentUser);
        Note note = requireNote(access.workspace().id(), noteId);
        noteRepository.softDelete(note.workspaceId(), note.id(), Instant.now());
    }

    private Note requireNote(UUID workspaceId, UUID noteId) {
        Note note = noteRepository.findById(workspaceId, noteId).orElseThrow(ResourceNotFoundException::note);
        fileLifecycle.checkParentWrite(workspaceId, note.projectId());
        return note;
    }

    private UUID resolveProjectId(UUID workspaceId, UUID projectId) {
        if (projectId == null) {
            return null;
        }
        var project = projectService.requireProject(workspaceId, projectId);
        projectService.requireAssignable(project);
        return project.id();
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return DEFAULT_TITLE;
        }
        return title.trim();
    }
}
