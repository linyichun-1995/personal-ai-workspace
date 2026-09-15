package com.example.workspace.workspace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workspace.common.util.UuidV7;
import com.example.workspace.workspace.domain.WorkspaceMember;
import com.example.workspace.workspace.domain.WorkspaceRole;
import com.example.workspace.workspace.repository.WorkspaceMemberRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class WorkspaceApiIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    WorkspaceMemberRepository workspaceMemberRepository;

    @Test
    void ownerCanReadAndUpdateWorkspace() throws Exception {
        RegisteredUser owner = register("ws-owner@example.com", "Owner");

        mockMvc.perform(get("/api/v1/workspaces").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].role").value("OWNER"));

        String workspaceId = owner.body().get("workspace").get("id").asText();
        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("我的工作空间"))
                .andExpect(jsonPath("$.timezone").value("UTC"))
                .andExpect(jsonPath("$.weekStartsOn").value(1))
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Space","timezone":"Asia/Shanghai","weekStartsOn":7,"version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada Space"))
                .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.weekStartsOn").value(7))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void nonMemberCannotReadOrUpdateWorkspace() throws Exception {
        RegisteredUser owner = register("ws-a@example.com", "A");
        RegisteredUser stranger = register("ws-b@example.com", "B");
        String workspaceId = owner.body().get("workspace").get("id").asText();

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId).header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", bearer(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hijack","timezone":"UTC","weekStartsOn":1,"version":0}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/workspaces/" + UUID.randomUUID()).header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void memberCanReadButCannotPatch() throws Exception {
        RegisteredUser owner = register("ws-member-owner@example.com", "Owner");
        RegisteredUser member = register("ws-member@example.com", "Member");
        UUID workspaceId = UUID.fromString(owner.body().get("workspace").get("id").asText());
        UUID memberUserId = UUID.fromString(member.body().get("user").get("id").asText());

        workspaceMemberRepository.insert(new WorkspaceMember(
                UuidV7.next(),
                workspaceId,
                memberUserId,
                WorkspaceRole.MEMBER,
                Instant.now()
        ));

        mockMvc.perform(get("/api/v1/workspaces/" + workspaceId).header("Authorization", bearer(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(patch("/api/v1/workspaces/" + workspaceId)
                        .header("Authorization", bearer(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"No","timezone":"UTC","weekStartsOn":1,"version":0}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private RegisteredUser register(String email, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"password123","name":"%s"}
                                """.formatted(email, name)))
                .andExpect(status().isCreated())
                .andReturn();
        return new RegisteredUser(
                jsonMapper.readTree(result.getResponse().getContentAsString()),
                requireRefreshCookie(result)
        );
    }

    private static String bearer(RegisteredUser registered) {
        return "Bearer " + registered.body().get("accessToken").asText();
    }

    private static MockCookie requireRefreshCookie(MvcResult result) {
        MockCookie cookie = (MockCookie) result.getResponse().getCookie("refresh_token");
        if (cookie != null) {
            return cookie;
        }
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        if (header == null) {
            throw new AssertionError("Missing refresh_token cookie");
        }
        return MockCookie.parse(header);
    }

    private record RegisteredUser(JsonNode body, MockCookie cookie) {
    }
}
