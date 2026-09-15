package com.example.workspace.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

class TaskApiIT extends ApiIT {

    @Test
    void canCreateAdvanceCompleteAndReopenTask() throws Exception {
        RegisteredUser owner = register("task-owner@example.com", "Owner");
        String projectId = createProject(owner, "任务项目");

        String created = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","title":"写方案","priority":"HIGH"}
                                """.formatted(projectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").isEmpty())
                .andExpect(jsonPath("$.projectName").value("任务项目"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String taskId = jsonMapper.readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"IN_PROGRESS","version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").isEmpty());

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DONE","version":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/tasks/" + taskId + "/status")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"TODO","version":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").isEmpty());
    }

    @Test
    void filtersTodayOverdueAndUpcoming() throws Exception {
        RegisteredUser owner = register("task-due@example.com", "Owner");
        Instant now = Instant.now();
        createTask(owner, "今天", now.plus(2, ChronoUnit.HOURS));
        createTask(owner, "逾期", now.minus(2, ChronoUnit.DAYS));
        createTask(owner, "即将到期", now.plus(3, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/v1/tasks").param("due", "TODAY").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("今天"));
        mockMvc.perform(get("/api/v1/tasks").param("due", "OVERDUE").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("逾期"));
        mockMvc.perform(get("/api/v1/tasks").param("due", "UPCOMING").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("即将到期"));
    }

    @Test
    void otherUserCannotReadTask() throws Exception {
        RegisteredUser owner = register("task-a@example.com", "A");
        RegisteredUser stranger = register("task-b@example.com", "B");
        String taskId = createTask(owner, "隔离任务", Instant.now().plus(1, ChronoUnit.HOURS));
        mockMvc.perform(get("/api/v1/tasks/" + taskId).header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingParentSoftDeletesChildren() throws Exception {
        RegisteredUser owner = register("task-parent@example.com", "Owner");
        String parentId = createTask(owner, "父任务", null);
        String child = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":"%s","title":"子任务"}
                                """.formatted(parentId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String childId = jsonMapper.readTree(child).get("id").asText();

        mockMvc.perform(delete("/api/v1/tasks/" + parentId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/tasks/" + childId).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    private String createProject(RegisteredUser owner, String name) throws Exception {
        String created = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return jsonMapper.readTree(created).get("id").asText();
    }

    private String createTask(RegisteredUser owner, String title, Instant dueAt) throws Exception {
        String dueJson = dueAt == null ? "" : ",\"dueAt\":\"" + dueAt + "\"";
        String created = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s"%s}
                                """.formatted(title, dueJson)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return jsonMapper.readTree(created).get("id").asText();
    }
}
