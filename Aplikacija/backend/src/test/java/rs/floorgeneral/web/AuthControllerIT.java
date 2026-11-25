package rs.floorgeneral.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import rs.floorgeneral.AbstractIntegrationTest;

/** Podrazumevani admin nalog (admin/admin123) pravi AdminSeeder pri startu konteksta. */
class AuthControllerIT extends AbstractIntegrationTest {

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        String requestBody = """
                {"username": "admin", "password": "admin123"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.expiresInSeconds").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        String requestBody = """
                {"username": "admin", "password": "pogresna-lozinka"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_withUnknownUsername_returns401() throws Exception {
        String requestBody = """
                {"username": "ne-postoji", "password": "bilo-sta"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withBlankUsername_returns400() throws Exception {
        String requestBody = """
                {"username": "", "password": "admin123"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty());
    }

    @Test
    void protectedEndpoint_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/leagues").header(HttpHeaders.AUTHORIZATION, "Bearer ovo-nije-validan-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
