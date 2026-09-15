package com.example.workspace.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workspace.support.ApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class NoteApiIT extends ApiIT {

    @Test
    void canCreateSearchFavoriteAndAutosaveNote() throws Exception {
        RegisteredUser owner = register("note-owner@example.com", "Owner");
        String projectId = jsonMapper.readTree(mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"笔记项目"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("id").asText();

        String created = mockMvc.perform(post("/api/v1/notes")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"方案","content":"# 背景\\n\\n第一版内容"}
                                """.formatted(projectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("方案"))
                .andExpect(jsonPath("$.content").value("# 背景\n\n第一版内容"))
                .andExpect(jsonPath("$.summary").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String noteId = jsonMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/v1/notes").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].content").value(org.hamcrest.Matchers.nullValue()));

        mockMvc.perform(get("/api/v1/notes").param("keyword", "背景").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(put("/api/v1/notes/" + noteId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"方案","content":"更新后的正文","favorite":true,"archived":false,"version":0}
                                """.formatted(projectId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put("/api/v1/notes/" + noteId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"方案","content":"冲突","favorite":true,"archived":false,"version":0}
                                """.formatted(projectId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void otherUserCannotReadNote() throws Exception {
        RegisteredUser owner = register("note-a@example.com", "A");
        RegisteredUser stranger = register("note-b@example.com", "B");
        String created = mockMvc.perform(post("/api/v1/notes")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"私密"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String noteId = jsonMapper.readTree(created).get("id").asText();
        mockMvc.perform(get("/api/v1/notes/" + noteId).header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/notes/" + noteId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
    }
}
