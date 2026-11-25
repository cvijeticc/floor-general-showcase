package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.domain.ApplicationStatus;
import rs.floorgeneral.domain.SkillLevel;
import rs.floorgeneral.web.dto.application.ApplicationResult;
import rs.floorgeneral.web.dto.player.PlayerInfo;

/** D-20 — prijava sa javnog sajta → lista u adminu → igrac; plus izvoz u Excel. */
class ApplicationControllerIT extends AbstractIntegrationTest {

    private static final String VALID = """
            {"fullName": "Stefan Stefanović", "nickname": "Stele", "heightCm": 192, "weightKg": 88,
             "birthYear": 1999, "phone": "+381 64 1234567", "email": "stele@example.com",
             "skillLevel": "TRAINED_BEFORE", "note": "Ne mogu nedeljom", "about": null, "website": ""}
            """;

    @Test
    void submit_withoutToken_isStoredAsNewApplication() throws Exception {
        submit(VALID);

        ApplicationResult[] list = list(adminAuthHeader());
        assertThat(list).hasSize(1);
        assertThat(list[0].fullName()).isEqualTo("Stefan Stefanović");
        assertThat(list[0].nickname()).isEqualTo("Stele");
        assertThat(list[0].skillLevel()).isEqualTo(SkillLevel.TRAINED_BEFORE);
        assertThat(list[0].status()).isEqualTo(ApplicationStatus.NEW);
        assertThat(list[0].createdAt()).isNotNull();
    }

    @Test
    void submit_withHoneypotFilled_answersCreatedButStoresNothing() throws Exception {
        submit(VALID.replace("\"website\": \"\"", "\"website\": \"http://spam.example\""));

        assertThat(list(adminAuthHeader())).isEmpty();
    }

    @Test
    void submit_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/public/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\": \"\", \"skillLevel\": \"PRO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void accept_createsPlayerFromApplication_andSecondAcceptIsConflict() throws Exception {
        String auth = adminAuthHeader();
        submit(VALID);
        Long id = list(auth)[0].id();

        MvcResult result = mockMvc.perform(post("/api/v1/applications/{id}/accept", id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        ApplicationResult accepted = objectMapper.readValue(result.getResponse().getContentAsString(),
                ApplicationResult.class);
        assertThat(accepted.status()).isEqualTo(ApplicationStatus.ACCEPTED);
        assertThat(accepted.playerId()).isNotNull();

        MvcResult playerResult = mockMvc.perform(get("/api/v1/players/{id}", accepted.playerId())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        PlayerInfo player = objectMapper.readValue(playerResult.getResponse().getContentAsString(), PlayerInfo.class);
        assertThat(player.firstName()).isEqualTo("Stefan");
        assertThat(player.lastName()).isEqualTo("Stefanović");
        assertThat(player.nickname()).isEqualTo("Stele");
        assertThat(player.birthYear()).isEqualTo(1999);
        assertThat(player.note()).isEqualTo("Ne mogu nedeljom");

        mockMvc.perform(post("/api/v1/applications/{id}/accept", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    @Test
    void undoAfterAccept_removesPlayerAndReturnsApplicationToNew() throws Exception {
        String auth = adminAuthHeader();
        submit(VALID);
        Long id = list(auth)[0].id();
        MvcResult result = mockMvc.perform(post("/api/v1/applications/{id}/accept", id)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        Long playerId = objectMapper.readValue(result.getResponse().getContentAsString(), ApplicationResult.class)
                .playerId();

        mockMvc.perform(post("/api/v1/undo").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/players/{id}", playerId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
        ApplicationResult restored = list(auth)[0];
        assertThat(restored.status()).isEqualTo(ApplicationStatus.NEW);
        assertThat(restored.playerId()).isNull();
    }

    @Test
    void changeStatus_andDelete() throws Exception {
        String auth = adminAuthHeader();
        submit(VALID);
        Long id = list(auth)[0].id();

        mockMvc.perform(patch("/api/v1/applications/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CONTACTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"));

        mockMvc.perform(patch("/api/v1/applications/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACCEPTED\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/v1/applications/{id}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        assertThat(list(auth)).isEmpty();
    }

    @Test
    void exportXlsx_returnsWorkbookAsAttachment() throws Exception {
        String auth = adminAuthHeader();
        submit(VALID);

        MvcResult result = mockMvc.perform(get("/api/v1/export/xlsx").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("attachment; filename=\"floor-general-")))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        // .xlsx je ZIP arhiva — pocinje potpisom „PK".
        assertThat(body.length).isGreaterThan(1000);
        assertThat(new String(body, 0, 2)).isEqualTo("PK");
    }

    @Test
    void mailStatus_withoutSmtp_isDisabled_andTestMailIsConflict() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/applications/mail-status").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.recipients[0]").value("floorgeneral.rs@gmail.com"));

        mockMvc.perform(post("/api/v1/applications/mail-test").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SPRING_MAIL_HOST")));
    }

    @Test
    void mailStatus_withoutToken_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/applications/mail-status")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/applications/mail-test")).andExpect(status().isUnauthorized());
    }

    private void submit(String body) throws Exception {
        mockMvc.perform(post("/api/v1/public/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private ApplicationResult[] list(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/applications").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ApplicationResult[].class);
    }
}
