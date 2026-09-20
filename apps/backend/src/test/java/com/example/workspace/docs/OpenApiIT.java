package com.example.workspace.docs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workspace.support.ApiIT;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

class OpenApiIT extends ApiIT {

    private static final List<String> DOCUMENTED_PATHS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/me",
            "/api/v1/me/password",
            "/api/v1/workspaces",
            "/api/v1/workspaces/{workspaceId}",
            "/api/v1/projects",
            "/api/v1/projects/{projectId}",
            "/api/v1/projects/{projectId}/status",
            "/api/v1/projects/{projectId}/archive",
            "/api/v1/projects/{projectId}/restore",
            "/api/v1/tasks",
            "/api/v1/tasks/{taskId}",
            "/api/v1/tasks/{taskId}/status",
            "/api/v1/notes",
            "/api/v1/notes/{noteId}",
            "/api/v1/dashboard"
    );

    @Test
    void openApiSpecDocumentsAllHttpApis() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode spec = jsonMapper.readTree(result.getResponse().getContentAsString());
        JsonNode paths = spec.path("paths");

        for (String path : DOCUMENTED_PATHS) {
            assertTrue(paths.has(path), "missing OpenAPI path: " + path);
        }

        JsonNode securitySchemes = spec.path("components").path("securitySchemes");
        assertTrue(securitySchemes.has("bearerAuth"));
        assertTrue(securitySchemes.has("refreshCookie"));

        JsonNode meParams = spec.path("paths").path("/api/v1/me").path("get").path("parameters");
        for (JsonNode parameter : meParams) {
            String name = parameter.path("name").asText();
            assertFalse(name.equals("userId") || name.equals("email"), "CurrentUser leaked as query param: " + name);
        }

        JsonNode loginSecurity = spec.path("paths").path("/api/v1/auth/login").path("post").path("security");
        assertTrue(loginSecurity.isMissingNode() || loginSecurity.isEmpty() || loginSecurity.isNull());
    }

    @Test
    void scalarUiIsPublic() throws Exception {
        mockMvc.perform(get("/scalar"))
                .andExpect(status().isOk());
    }
}
