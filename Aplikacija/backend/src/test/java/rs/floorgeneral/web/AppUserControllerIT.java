package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.domain.UserRole;
import rs.floorgeneral.web.dto.user.UserInfo;
import rs.floorgeneral.web.dto.user.UserResult;

/**
 * UC-ADM-13. Jedini nalog na pocetku je „admin", koji pravi AdminSeeder pri podizanju konteksta.
 * Svaki test radi u transakciji koja se rolluje, pa promene lozinke ne cure u druge testove.
 */
class AppUserControllerIT extends AbstractIntegrationTest {

    @Test
    void search_returnsSeededAdminWithoutPasswordHash() throws Exception {
        String auth = adminAuthHeader();

        MvcResult result = mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("password").doesNotContain("$2a$");

        UserResult[] users = objectMapper.readValue(body, UserResult[].class);
        assertThat(users).hasSize(1);
        assertThat(users[0].username()).isEqualTo("admin");
        assertThat(users[0].role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void create_addsNewAdminAccount() throws Exception {
        String auth = adminAuthHeader();

        UserInfo created = createUser(auth, "organizator", "tajnalozinka");

        assertThat(created.id()).isNotNull().isPositive();
        assertThat(created.role()).isEqualTo(UserRole.ADMIN);
        assertThat(users(auth)).hasSize(2);
    }

    @Test
    void create_withDuplicateUsername_returns409() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"tajnalozinka\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void create_withShortPassword_returns400() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"organizator\", \"password\": \"kratka\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_withBlankUsername_returns400() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"\", \"password\": \"tajnalozinka\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty());
    }

    @Test
    void changeOwnPassword_withCorrectOldPassword_allowsLoginWithNewOne() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\": \"admin123\", \"newPassword\": \"novatajna123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"admin\", \"password\": \"novatajna123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changeOwnPassword_withWrongOldPassword_returns400() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\": \"pogresna\", \"newPassword\": \"novatajna123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void resetPassword_setsNewPasswordForAnotherAccount() throws Exception {
        String auth = adminAuthHeader();
        UserInfo created = createUser(auth, "organizator", "tajnalozinka");

        mockMvc.perform(patch("/api/v1/users/{id}/password", created.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\": \"resetovana123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"organizator\", \"password\": \"resetovana123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_lastAdmin_returns409() throws Exception {
        String auth = adminAuthHeader();
        UserResult admin = users(auth)[0];

        mockMvc.perform(delete("/api/v1/users/{id}", admin.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void delete_whenAnotherAdminExists_removesAccount() throws Exception {
        String auth = adminAuthHeader();
        UserInfo created = createUser(auth, "organizator", "tajnalozinka");

        mockMvc.perform(delete("/api/v1/users/{id}", created.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        assertThat(users(auth)).hasSize(1);
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(delete("/api/v1/users/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    private UserResult[] users(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserResult[].class);
    }

    private UserInfo createUser(String auth, String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserInfo.class);
    }
}
