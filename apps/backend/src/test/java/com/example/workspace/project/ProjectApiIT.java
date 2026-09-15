package com.example.workspace.project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workspace.support.ApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ProjectApiIT extends ApiIT {

    @Test
    void ownerCanCreateListUpdateArchiveAndDeleteProject() throws Exception {
        RegisteredUser owner = register("project-owner@example.com", "Owner");

        String created = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"知识库","description":"个人知识","status":"ACTIVE","priority":"HIGH","startDate":"2026-09-01","dueDate":"2026-09-30"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("知识库"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.stats.taskCount").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String projectId = jsonMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].name").value("知识库"));

        mockMvc.perform(put("/api/v1/projects/" + projectId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"知识库 2","description":"更新","status":"PAUSED","priority":"MEDIUM","startDate":"2026-09-01","dueDate":"2026-10-01","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("知识库 2"))
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/api/v1/projects/" + projectId + "/status")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"COMPLETED","version":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/archive")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/projects").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.items.length()").value(0));
        mockMvc.perform(get("/api/v1/projects").param("archived", "true").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/restore")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archivedAt").isEmpty());

        mockMvc.perform(delete("/api/v1/projects/" + projectId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUserCannotReadProject() throws Exception {
        RegisteredUser owner = register("project-a@example.com", "A");
        RegisteredUser stranger = register("project-b@example.com", "B");
        String created = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"私有项目"}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String projectId = jsonMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidDates() throws Exception {
        RegisteredUser owner = register("project-dates@example.com", "Owner");
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"日期错误","startDate":"2026-09-30","dueDate":"2026-09-01"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }
}
