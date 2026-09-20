package com.example.workspace.search;

import static com.example.workspace.infrastructure.database.jooq.Tables.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.workspace.common.domain.SourceType;
import com.example.workspace.job.JobWorker;
import com.example.workspace.search.application.SearchIndexer;
import com.example.workspace.support.ApiIT;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;

@TestPropertySource(properties = "app.jobs.enabled=false")
class SearchApiIT extends ApiIT {
    @Autowired DSLContext db;
    @Autowired SearchIndexer indexer;
    @Autowired PlatformTransactionManager transactions;

    private JobWorker worker() { return new JobWorker(db, transactions, indexer); }
    private JsonNode create(String token, String resource, Map<String, ?> body) throws Exception {
        return jsonMapper.readTree(mockMvc.perform(post("/api/v1/" + resource).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
    private ResultActions search(String token, String q) throws Exception {
        return mockMvc.perform(get("/api/v1/search").header("Authorization", token).param("q", q));
    }

    @Test void shortChineseUnicodeAndLiteralTermsUseScopedIndexAndUsefulOriginalSnippets() throws Exception {
        String token = bearer(register(UUID.randomUUID() + "@example.com", "搜索"));
        String other = bearer(register(UUID.randomUUID() + "@example.com", "其他"));
        String raw = "开头".repeat(200) + "\n  ＡＩ 缓存 Cafe\u0301 😀方案 %_";
        var note = create(token, "notes", Map.of("title", "缓存设计", "content", raw));
        create(token, "notes", Map.of("title", "普通文档", "content", "只有其他文字"));
        create(other, "notes", Map.of("title", "缓存设计", "content", raw));
        search(token, "缓存").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.meta.indexingPending").value(true));
        worker().tick();
        search(token, "缓存").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(note.path("id").asText()))
                .andExpect(jsonPath("$.items[0].matchedFields.length()").value(2))
                .andExpect(jsonPath("$.meta.indexingPending").value(false));
        var result = search(token, "ai café 😀方 %_").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andReturn().getResponse().getContentAsString();
        JsonNode snippet = jsonMapper.readTree(result).path("items").get(0).path("snippet");
        StringBuilder displayed = new StringBuilder();
        StringBuilder matches = new StringBuilder();
        snippet.forEach(segment -> {
            displayed.append(segment.path("text").asText());
            if (segment.path("matched").asBoolean()) matches.append(segment.path("text").asText());
        });
        assertTrue(raw.contains(displayed));
        assertTrue(displayed.toString().contains("ＡＩ"));
        assertEquals("ＡＩCafe\u0301😀方%_", matches.toString());
        assertTrue(displayed.codePointCount(0, displayed.length()) <= 240);
        search(token, "缓").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SEARCH_QUERY_INVALID"));
        search(token, "").andExpect(status().isOk()).andExpect(jsonPath("$.meta.emptyQuery").value(true));
        mockMvc.perform(get("/api/v1/search").header("Authorization", token).param("q", "缓存").param("page", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test void tagsArchiveDeleteAndProjectScopeAreLiveEvenBeforeReindexing() throws Exception {
        String token = bearer(register(UUID.randomUUID() + "@example.com", "实时搜索"));
        var project = create(token, "projects", Map.of("name", "缓存项目"));
        UUID projectId = UUID.fromString(project.path("id").asText());
        var note = create(token, "notes", Map.of("title", "缓存笔记", "projectId", projectId));
        String noteId = note.path("id").asText();
        var tag = create(token, "tags", Map.of("name", "实时标签"));
        String tagId = tag.path("id").asText();
        worker().tick();
        mockMvc.perform(get("/api/v1/search").header("Authorization", token).param("q", "缓存").param("projectId", projectId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        mockMvc.perform(put("/api/v1/notes/" + noteId + "/tags").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tagIds\":[\"" + tagId + "\"],\"tagVersion\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/search").header("Authorization", token).param("tagIds", tagId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].tags[0].name").value("实时标签"));
        db.update(PROJECTS).set(PROJECTS.ARCHIVED_AT, OffsetDateTime.now()).where(PROJECTS.ID.eq(projectId)).execute();
        search(token, "缓存").andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/api/v1/search").header("Authorization", token).param("q", "缓存").param("includeArchived", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items[0].archived").value(true));
        db.update(PROJECTS).set(PROJECTS.DELETED_AT, OffsetDateTime.now()).where(PROJECTS.ID.eq(projectId)).execute();
        mockMvc.perform(get("/api/v1/search").header("Authorization", token).param("q", "缓存").param("includeArchived", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/api/v1/tags").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].referenceCount").value(0));
    }

    @Test void restartedWorkersReclaimExpiredLeasesAndReadTheLatestSourceIdempotently() throws Exception {
        String token = bearer(register(UUID.randomUUID() + "@example.com", "重启"));
        var note = create(token, "notes", Map.of("title", "旧标题"));
        UUID id = UUID.fromString(note.path("id").asText());
        var j = BACKGROUND_JOBS;
        UUID jobId = db.select(j.ID).from(j).where(j.SOURCE_ID.eq(id)).fetchSingle(j.ID);
        db.update(j).set(j.STATUS, "RUNNING").set(j.ATTEMPTS, 1).set(j.LEASE_TOKEN, UUID.randomUUID())
                .set(j.LEASE_UNTIL, OffsetDateTime.now().minusMinutes(1)).where(j.ID.eq(jobId)).execute();
        mockMvc.perform(put("/api/v1/notes/" + id).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"最新缓存\",\"version\":0}")).andExpect(status().isOk());
        worker().tick();
        worker().tick();
        assertEquals("SUCCEEDED", db.select(j.STATUS).from(j).where(j.ID.eq(jobId)).fetchSingle(j.STATUS));
        assertEquals(1, db.fetchCount(SEARCH_DOCUMENTS, SEARCH_DOCUMENTS.SOURCE_ID.eq(id)));
        search(token, "旧标题").andExpect(jsonPath("$.total").value(0));
        search(token, "最新缓存").andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(delete("/api/v1/notes/" + id).header("Authorization", token)).andExpect(status().isNoContent());
        search(token, "最新缓存").andExpect(jsonPath("$.total").value(0));
        worker().tick();
        assertEquals(0, db.fetchCount(SEARCH_DOCUMENTS, SEARCH_DOCUMENTS.SOURCE_ID.eq(id)));
    }

    @Test void expiredFinalAttemptsBecomeDeadAndLostLeasesCannotCommitProjection() throws Exception {
        String token = bearer(register(UUID.randomUUID() + "@example.com", "租约"));
        var note = create(token, "notes", Map.of("title", "租约缓存"));
        UUID id = UUID.fromString(note.path("id").asText());
        var j = BACKGROUND_JOBS;
        UUID jobId = db.select(j.ID).from(j).where(j.SOURCE_ID.eq(id)).fetchSingle(j.ID);
        SearchIndexer expiredDuringIndex = new SearchIndexer(db) {
            @Override public void index(UUID workspace, SourceType type, UUID source, long generation) {
                super.index(workspace, type, source, generation);
                db.update(j).set(j.LEASE_UNTIL, OffsetDateTime.now().minusSeconds(1)).where(j.SOURCE_ID.eq(source)).execute();
            }
        };
        new JobWorker(db, transactions, expiredDuringIndex).tick();
        assertEquals(0, db.fetchCount(SEARCH_DOCUMENTS, SEARCH_DOCUMENTS.SOURCE_ID.eq(id)));
        assertEquals("RUNNING", db.select(j.STATUS).from(j).where(j.ID.eq(jobId)).fetchSingle(j.STATUS));
        db.update(j).set(j.ATTEMPTS, 5).set(j.LEASE_UNTIL, OffsetDateTime.now().minusSeconds(1)).where(j.ID.eq(jobId)).execute();
        worker().tick();
        var failed = db.selectFrom(j).where(j.ID.eq(jobId)).fetchSingle();
        assertEquals("DEAD", failed.getStatus());
        assertNull(failed.getLeaseToken());
        assertNull(failed.getLeaseUntil());
        assertEquals(0, db.fetchCount(SEARCH_DOCUMENTS, SEARCH_DOCUMENTS.SOURCE_ID.eq(id)));
        search(token, "租约缓存").andExpect(status().isOk()).andExpect(jsonPath("$.meta.indexingPending").value(true));
    }
}
