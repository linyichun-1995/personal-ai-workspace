package com.example.workspace.note.controller;

import com.example.workspace.common.api.PageQuery;
import com.example.workspace.common.api.PageResponse;
import com.example.workspace.common.security.CurrentUser;
import com.example.workspace.note.application.NoteService;
import com.example.workspace.note.dto.CreateNoteRequest;
import com.example.workspace.note.dto.NoteResponse;
import com.example.workspace.note.dto.UpdateNoteRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(CurrentUser currentUser, @Valid @RequestBody CreateNoteRequest request) {
        return noteService.create(currentUser, request);
    }

    @GetMapping
    public PageResponse<NoteResponse> list(
            CurrentUser currentUser,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String keyword,
            @Valid PageQuery pageQuery
    ) {
        return noteService.list(currentUser, projectId, favorite, archived, keyword, pageQuery);
    }

    @GetMapping("/{noteId}")
    public NoteResponse get(CurrentUser currentUser, @PathVariable UUID noteId) {
        return noteService.get(currentUser, noteId);
    }

    @PutMapping("/{noteId}")
    public NoteResponse update(
            CurrentUser currentUser,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request
    ) {
        return noteService.update(currentUser, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(CurrentUser currentUser, @PathVariable UUID noteId) {
        noteService.delete(currentUser, noteId);
    }
}
