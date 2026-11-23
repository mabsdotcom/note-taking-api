package com.notesapp.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class NoteControllerIntegrationTest extends IntegrationTestSupport {

    private String token;

    @BeforeEach
    void login() throws Exception {
        token = registerAndGetToken();
    }

    private Integer createCategory(String name) throws Exception {
        String body = mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", name, "color", "#1E88E5"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private Integer createNote(String title, String content, Integer categoryId, List<String> tags) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        request.put("content", content);
        request.put("categoryId", categoryId);
        request.put("tags", tags);
        String body = mockMvc.perform(post("/api/notes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void createAndGetNote_sanitizesHtml_andAuditsCreator() throws Exception {
        Integer categoryId = createCategory("Work");
        Integer id = createNote("Plan", "<p>Hi <b>there</b></p><script>alert(1)</script>", categoryId,
                List.of("Work", "work", "ideas"));

        mockMvc.perform(get("/api/notes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Plan"))
                .andExpect(jsonPath("$.content").value("<p>Hi <b>there</b></p>"))
                .andExpect(jsonPath("$.category.name").value("Work"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.createdBy").value(containsString("user_")))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createNote_withoutTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "<p>no title</p>"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.title").exists());
    }

    @Test
    void search_filtersByTextTagCategory_andPaginates() throws Exception {
        Integer work = createCategory("Work");
        createNote("Budget meeting", "<p>Discuss Q3 numbers</p>", work, List.of("meeting"));
        createNote("Groceries", "<p>milk, eggs</p>", null, List.of("shopping"));
        createNote("Design review", "<p>The meeting went well</p>", work, List.of("design"));

        // full-text search in title OR body
        mockMvc.perform(get("/api/notes").param("q", "MEETING").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // tag filter
        mockMvc.perform(get("/api/notes").param("tags", "shopping").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Groceries"));

        // category filter + sort + pagination
        mockMvc.perform(get("/api/notes")
                        .param("categoryId", work.toString())
                        .param("sort", "title,asc")
                        .param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Budget meeting"));
    }

    @Test
    void search_withInvalidDateRangeOrSort_returns400() throws Exception {
        mockMvc.perform(get("/api/notes")
                        .param("createdFrom", "2026-05-10")
                        .param("createdTo", "2026-05-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.createdTo").exists());

        mockMvc.perform(get("/api/notes").param("sort", "password").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notesOfAnotherUser_areNotVisible() throws Exception {
        Integer id = createNote("Private", "<p>secret</p>", null, List.of());
        String otherToken = registerAndGetToken();

        mockMvc.perform(get("/api/notes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(otherToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_thenSoftDelete_trash_restore_andHistory() throws Exception {
        Integer id = createNote("Draft", "<p>v1</p>", null, List.of());

        mockMvc.perform(put("/api/notes/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Final", "content", "<p>v2</p>", "pinned", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.pinned").value(true));

        mockMvc.perform(delete("/api/notes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/notes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/notes").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(id))));
        mockMvc.perform(get("/api/notes/trash").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.content[*].id", hasItem(id)));

        mockMvc.perform(post("/api/notes/{id}/restore", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"));

        mockMvc.perform(get("/api/notes/{id}/history", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].action", hasItem("RESTORED")));
    }

    @Test
    void deletePermanently_removesNote() throws Exception {
        Integer id = createNote("Temp", "", null, List.of("tmp"));

        mockMvc.perform(delete("/api/notes/{id}/permanent", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/notes/{id}/restore", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingCategory_keepsNotesUncategorised() throws Exception {
        Integer categoryId = createCategory("Temp");
        Integer noteId = createNote("Keep me", "", categoryId, List.of());

        mockMvc.perform(delete("/api/categories/{id}", categoryId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notes/{id}", noteId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").doesNotExist());
    }

    @Test
    void exportCsv_returnsCsvAttachment() throws Exception {
        createNote("Export me", "<p>hello</p>", null, List.of("a"));

        mockMvc.perform(get("/api/notes/export").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Export me")));
    }

    @Test
    void adminEndpoints_forbiddenForUsers_allowedForAdmins() throws Exception {
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());

        Integer id = createNote("Lost", "", null, List.of());
        mockMvc.perform(delete("/api/notes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        String adminToken = createAdminAndGetToken();
        mockMvc.perform(get("/api/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").isNumber());
        mockMvc.perform(post("/api/admin/notes/{id}/restore", id).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/notes/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }
}
