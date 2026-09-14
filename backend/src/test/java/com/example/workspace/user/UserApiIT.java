package com.example.workspace.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class UserApiIT {

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

    @Test
    void meIncludesLocaleTimezoneAndVersion() throws Exception {
        RegisteredUser registered = register("profile-me@example.com", "password123", "Ada", "Asia/Shanghai");

        mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(registered)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada"))
                .andExpect(jsonPath("$.locale").value("zh-CN"))
                .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void patchProfileUpdatesFieldsAndRejectsInvalidInput() throws Exception {
        RegisteredUser registered = register("profile-patch@example.com", "password123", "Ada", null);

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", bearer(registered))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"Ada Lovelace",
                                  "avatarUrl":"https://cdn.example.com/ada.png",
                                  "locale":"en-US",
                                  "timezone":"Europe/London",
                                  "version":0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada Lovelace"))
                .andExpect(jsonPath("$.avatarUrl").value("https://cdn.example.com/ada.png"))
                .andExpect(jsonPath("$.locale").value("en-US"))
                .andExpect(jsonPath("$.timezone").value("Europe/London"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", bearer(registered))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"已更新用户",
                                  "avatarUrl":null,
                                  "locale":"zh-CN",
                                  "timezone":"UTC",
                                  "version":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("已更新用户"))
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", bearer(registered))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"",
                                  "locale":"en-US",
                                  "timezone":"Europe/London",
                                  "version":1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", bearer(registered))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"Ada",
                                  "avatarUrl":"ftp://bad.example",
                                  "locale":"en-US",
                                  "timezone":"Europe/London",
                                  "version":1
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", bearer(registered))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName":"Stale",
                                  "locale":"zh-CN",
                                  "timezone":"UTC",
                                  "version":0
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void changePasswordRevokesOtherSessions() throws Exception {
        RegisteredUser first = register("pwd@example.com", "password123", "Pwd", null);

        MvcResult secondLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pwd@example.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode secondBody = jsonMapper.readTree(secondLogin.getResponse().getContentAsString());
        MockCookie secondCookie = requireRefreshCookie(secondLogin);
        String secondAccess = secondBody.get("accessToken").asText();

        MvcResult changed = mockMvc.perform(put("/api/v1/me/password")
                        .header("Authorization", "Bearer " + secondAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"password456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();
        JsonNode changedBody = jsonMapper.readTree(changed.getResponse().getContentAsString());
        MockCookie newCookie = requireRefreshCookie(changed);

        mockMvc.perform(get("/api/v1/me").header("Authorization", bearer(first)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(first.cookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(secondCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + changedBody.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("pwd@example.com"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(newCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() throws Exception {
        RegisteredUser registered = register("pwd-wrong@example.com", "password123", "Pwd", null);

        mockMvc.perform(put("/api/v1/me/password")
                        .header("Authorization", bearer(registered))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"nope-nope","newPassword":"password456"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    private RegisteredUser register(String email, String password, String name, String timezone) throws Exception {
        String timezoneJson = timezone == null ? "" : ",\"timezone\":\"%s\"".formatted(timezone);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","name":"%s"%s}
                                """.formatted(email, password, name, timezoneJson)))
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
