package com.example.workspace.tag;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.workspace.support.ApiIT;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TagApiIT extends ApiIT {
    @Test void tagsAreScopedVersionedAndIndependentOfNoteContent() throws Exception {
        var user = register(UUID.randomUUID()+"@example.com","标签用户");
        var other = register(UUID.randomUUID()+"@example.com","其他用户");
        String token = bearer(user);
        var response = mockMvc.perform(post("/api/v1/tags").header("Authorization",token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"ＡＩ 方案\",\"color\":\"BLUE\"}"))
                .andExpect(status().isCreated()).andReturn();
        String tag = jsonMapper.readTree(response.getResponse().getContentAsString()).path("id").asText();
        mockMvc.perform(post("/api/v1/tags").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"ai 方案\"}")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TAG_NAME_CONFLICT"));
        response = mockMvc.perform(post("/api/v1/notes").header("Authorization",token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"测试笔记\"}")).andExpect(status().isCreated()).andReturn();
        String note = jsonMapper.readTree(response.getResponse().getContentAsString()).path("id").asText();
        String path = "/api/v1/notes/"+note+"/tags";
        String body = "{\"tagIds\":[\""+tag+"\"],\"tagVersion\":0}";
        mockMvc.perform(put(path).header("Authorization",bearer(other)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNotFound());
        mockMvc.perform(put(path).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tagVersion").value(1));
        mockMvc.perform(put(path).header("Authorization",token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("TAG_VERSION_CONFLICT"));
        mockMvc.perform(get("/api/v1/notes/"+note).header("Authorization",token)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0));
        mockMvc.perform(delete("/api/v1/tags/"+tag).header("Authorization",token).header("If-Match","0")).andExpect(status().isNoContent());
        mockMvc.perform(get(path).header("Authorization",token)).andExpect(jsonPath("$.tagVersion").value(2)).andExpect(jsonPath("$.tags").isEmpty());
        mockMvc.perform(get("/api/v1/notes/"+note).header("Authorization",token)).andExpect(status().isOk());
    }
}
