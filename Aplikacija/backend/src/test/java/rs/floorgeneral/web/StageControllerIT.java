package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.domain.StageStatus;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.web.dto.stage.StageInfo;
import rs.floorgeneral.web.dto.stage.StageResult;

/**
 * Test seed ima sezonu "Test sezona" sa jednom ACTIVE grupnom fazom koja vec ima grupu,
 * igrace i jedan odigran mec — na njoj se proveravaju zakljucani parametri i odbijeno brisanje.
 * Provere formata (FR-4.2) rade se na novoj sezoni, da seed ostane netaknut.
 */
class StageControllerIT extends AbstractIntegrationTest {

    @Test
    void searchBySeason_returnsSeededStage() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = seededSeasonId(auth);

        StageResult[] stages = stagesOf(auth, seasonId);

        assertThat(stages).hasSize(1);
        assertThat(stages[0].name()).isEqualTo("Grupna faza");
        assertThat(stages[0].type()).isEqualTo(StageType.GROUP);
        assertThat(stages[0].status()).isEqualTo(StageStatus.ACTIVE);
    }

    @Test
    void searchBySeason_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/seasons/{id}/stages", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void findById_returnsFormatParameters() throws Exception {
        String auth = adminAuthHeader();
        StageResult seeded = stagesOf(auth, seededSeasonId(auth))[0];

        MvcResult result = mockMvc.perform(get("/api/v1/stages/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        StageInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), StageInfo.class);
        assertThat(info.groupCount()).isEqualTo(1);
        assertThat(info.groupSize()).isEqualTo(4);
        assertThat(info.roundCount()).isEqualTo(2);
        assertThat(info.directAdvance()).isEqualTo(2);
        assertThat(info.bracketSize()).isNull();
        assertThat(info.seasonName()).isEqualTo(SEEDED_SEASON);
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/stages/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_groupStageWithFullFormat_isPersistedAsDraft() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona za faze 1");

        StageInfo created = createStage(auth, seasonId, """
                {"name": "Grupna faza", "type": "GROUP", "orderNo": 1, "groupCount": 10,
                 "groupSize": 10, "roundCount": 2, "directAdvance": 6, "additionalAdvance": 4}
                """);

        assertThat(created.status()).isEqualTo(StageStatus.DRAFT);
        assertThat(created.groupCount()).isEqualTo(10);
        assertThat(created.additionalAdvance()).isEqualTo(4);
        assertThat(created.seasonId()).isEqualTo(seasonId);
    }

    @Test
    void create_withDuplicateOrderNo_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = seededSeasonId(auth);

        mockMvc.perform(post("/api/v1/seasons/{id}/stages", seasonId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Jos jedna prva faza", "type": "KNOCKOUT", "orderNo": 1, "bracketSize": 8}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void create_groupStageWithoutGroupCount_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona za faze 2");

        mockMvc.perform(post("/api/v1/seasons/{id}/stages", seasonId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Grupna faza", "type": "GROUP", "orderNo": 1, "groupSize": 10,
                                 "roundCount": 2, "directAdvance": 6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_knockoutWithBracketSizeNotPowerOfTwo_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona za faze 3");

        mockMvc.perform(post("/api/v1/seasons/{id}/stages", seasonId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Playoff", "type": "KNOCKOUT", "orderNo": 1, "bracketSize": 48}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona za faze 4");

        mockMvc.perform(post("/api/v1/seasons/{id}/stages", seasonId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "type": "KNOCKOUT", "orderNo": 1, "bracketSize": 64}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void update_activeStageWithPlayedMatch_returns409() throws Exception {
        String auth = adminAuthHeader();
        StageResult seeded = stagesOf(auth, seededSeasonId(auth))[0];

        mockMvc.perform(put("/api/v1/stages/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Grupna faza", "orderNo": 1, "groupCount": 2, "groupSize": 4,
                                 "roundCount": 2, "directAdvance": 2, "additionalAdvance": 0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void update_draftStage_changesFormat() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona za faze 5");
        StageInfo stage = createStage(auth, seasonId, """
                {"name": "Grupna faza", "type": "GROUP", "orderNo": 1, "groupCount": 4,
                 "groupSize": 8, "roundCount": 1, "directAdvance": 4, "additionalAdvance": 0}
                """);

        MvcResult result = mockMvc.perform(put("/api/v1/stages/{id}", stage.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Grupna faza (izmenjena)", "orderNo": 1, "groupCount": 8,
                                 "groupSize": 8, "roundCount": 2, "directAdvance": 4, "additionalAdvance": 0}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        StageInfo updated = objectMapper.readValue(result.getResponse().getContentAsString(), StageInfo.class);
        assertThat(updated.name()).isEqualTo("Grupna faza (izmenjena)");
        assertThat(updated.groupCount()).isEqualTo(8);
        assertThat(updated.roundCount()).isEqualTo(2);
    }

    /** FR-4.2 — broj onih koji prolaze mora da popuni drvo sledece faze. */
    @Test
    void changeStatus_activatingGroupStage_checksAdvancingCountAgainstBracket() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona sa drvetom");
        StageInfo groupStage = createStage(auth, seasonId, """
                {"name": "Grupna faza", "type": "GROUP", "orderNo": 1, "groupCount": 2,
                 "groupSize": 4, "roundCount": 1, "directAdvance": 2, "additionalAdvance": 0}
                """);
        createStage(auth, seasonId, """
                {"name": "Playoff", "type": "KNOCKOUT", "orderNo": 2, "bracketSize": 8}
                """);

        // 2 grupe x 2 prolaznika = 4, a drvo prima 8 -> pokretanje se odbija.
        mockMvc.perform(patch("/api/v1/stages/{id}/status", groupStage.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(put("/api/v1/stages/{id}", groupStage.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Grupna faza", "orderNo": 1, "groupCount": 2, "groupSize": 4,
                                 "roundCount": 1, "directAdvance": 4, "additionalAdvance": 0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/stages/{id}/status", groupStage.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void changeStatus_backwards_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona za statuse");
        StageInfo stage = createStage(auth, seasonId, """
                {"name": "Playoff", "type": "KNOCKOUT", "orderNo": 1, "bracketSize": 16}
                """);

        mockMvc.perform(patch("/api/v1/stages/{id}/status", stage.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/stages/{id}/status", stage.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DRAFT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_stageWithGroups_returns409() throws Exception {
        String auth = adminAuthHeader();
        StageResult seeded = stagesOf(auth, seededSeasonId(auth))[0];

        mockMvc.perform(delete("/api/v1/stages/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_emptyStage_removesStage() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona za brisanje faze");
        StageInfo stage = createStage(auth, seasonId, """
                {"name": "Playoff", "type": "KNOCKOUT", "orderNo": 1, "bracketSize": 32}
                """);

        mockMvc.perform(delete("/api/v1/stages/{id}", stage.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/stages/{id}", stage.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    private StageResult[] stagesOf(String auth, Long seasonId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/seasons/{id}/stages", seasonId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), StageResult[].class);
    }

}
