package com.example.workspace.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workspace.support.ApiIT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class DashboardApiIT extends ApiIT {

    @Test
    void emptyDashboardThenCountsAfterWrites() throws Exception {
        RegisteredUser owner = register("dash-owner@example.com", "Owner");
        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.activeProjects").value(0))
                .andExpect(jsonPath("$.overview.todayTasks").value(0))
                .andExpect(jsonPath("$.overview.notes").value(0))
                .andExpect(jsonPath("$.overview.aiConversations").value(0))
                .andExpect(jsonPath("$.overview.reviewTasks").value(0))
                .andExpect(jsonPath("$.todayTasks.length()").value(0));

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"仪表盘项目","status":"ACTIVE"}
                                """))
                .andExpect(status().isCreated());

        Instant due = Instant.now().plus(1, ChronoUnit.HOURS);
        String task = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"今日任务","dueAt":"%s"}
                                """.formatted(due)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String taskId = jsonMapper.readTree(task).get("id").asText();

        mockMvc.perform(post("/api/v1/notes")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"最近笔记","content":"正文"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.activeProjects").value(1))
                .andExpect(jsonPath("$.overview.todayTasks").value(1))
                .andExpect(jsonPath("$.overview.notes").value(1))
                .andExpect(jsonPath("$.overview.notesThisWeek").value(1))
                .andExpect(jsonPath("$.todayTasks[0].title").value("今日任务"))
                .andExpect(jsonPath("$.recentNotes[0].title").value("最近笔记"))
                .andExpect(jsonPath("$.activeProjects[0].name").value("仪表盘项目"));

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE","version":0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.overview.todayTasks").value(0))
                .andExpect(jsonPath("$.overview.completedTasks").value(1))
                .andExpect(jsonPath("$.overview.completionRate").value(100));
    }

    @Test
    void dashboardIsIsolatedByWorkspace() throws Exception {
        RegisteredUser owner = register("dash-a@example.com", "A");
        RegisteredUser stranger = register("dash-b@example.com", "B");
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"A 的项目"}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearer(stranger)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.activeProjects").value(0));
    }
}
