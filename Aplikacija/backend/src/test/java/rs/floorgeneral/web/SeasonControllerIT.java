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
import rs.floorgeneral.domain.SeasonStatus;
import rs.floorgeneral.web.dto.season.SeasonInfo;
import rs.floorgeneral.web.dto.season.SeasonResult;

/**
 * Test seed (db/test/V4__seed_test_players.sql) ima tacno jednu sezonu — "Test sezona",
 * u statusu ACTIVE i sa jednom fazom. Zato se na njoj proveravaju i zakljucano bodovanje
 * (FR-4.1) i odbijeno brisanje sezone koja ima faze.
 */
class SeasonControllerIT extends AbstractIntegrationTest {

    @Test
    void search_returnsSeededSeason() throws Exception {
        String auth = adminAuthHeader();

        SeasonResult[] seasons = searchSeasons(auth, "");

        assertThat(seasons).hasSize(1);
        assertThat(seasons[0].name()).isEqualTo(SEEDED_SEASON);
        assertThat(seasons[0].status()).isEqualTo(SeasonStatus.ACTIVE);
        assertThat(seasons[0].leagueName()).isEqualTo(MAIN_LEAGUE);
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/seasons"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void search_byLeagueAndStatus_filtersResults() throws Exception {
        String auth = adminAuthHeader();
        Long mainLeagueId = leagueId(auth, MAIN_LEAGUE);
        Long otherLeagueId = leagueId(auth, "Floor General 1v1 Liga - Rekreativna divizija");

        assertThat(searchSeasons(auth, "?leagueId=" + mainLeagueId)).hasSize(1);
        assertThat(searchSeasons(auth, "?leagueId=" + otherLeagueId)).isEmpty();
        assertThat(searchSeasons(auth, "?status=DRAFT")).isEmpty();
        assertThat(searchSeasons(auth, "?status=ACTIVE")).hasSize(1);
    }

    @Test
    void findById_returnsScoringParameters() throws Exception {
        String auth = adminAuthHeader();
        SeasonResult seeded = searchSeasons(auth, "")[0];

        MvcResult result = mockMvc.perform(get("/api/v1/seasons/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        SeasonInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), SeasonInfo.class);
        assertThat(info.pointsForWin()).isEqualTo(2);
        assertThat(info.pointsForLoss()).isEqualTo(1);
        assertThat(info.pointsForForfeit()).isZero();
        assertThat(info.forfeitWinnerScore()).isEqualTo(11);
        assertThat(info.forfeitLoserScore()).isZero();
        assertThat(info.targetScore()).isEqualTo(11);
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/seasons/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutScoring_usesDefaultsAndStartsAsDraft() throws Exception {
        String auth = adminAuthHeader();
        Long leagueId = leagueId(auth, MAIN_LEAGUE);
        String body = """
                {"leagueId": %d, "name": "2026/2027", "startDate": "2026-09-19", "endDate": "2026-12-20", "playerCount": 100}
                """.formatted(leagueId);

        MvcResult result = mockMvc.perform(post("/api/v1/seasons")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        SeasonInfo created = objectMapper.readValue(result.getResponse().getContentAsString(), SeasonInfo.class);
        assertThat(created.status()).isEqualTo(SeasonStatus.DRAFT);
        assertThat(created.pointsForWin()).isEqualTo(2);
        assertThat(created.pointsForLoss()).isEqualTo(1);
        assertThat(created.targetScore()).isEqualTo(11);
        // D-18 — podrazumevano: najvise 12 poena, mec traje 8 minuta.
        assertThat(created.maxScore()).isEqualTo(12);
        assertThat(created.durationMinutes()).isEqualTo(8);
        assertThat(created.leagueName()).isEqualTo(MAIN_LEAGUE);
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        String auth = adminAuthHeader();
        String body = """
                {"leagueId": %d, "name": "", "playerCount": 100}
                """.formatted(leagueId(auth, MAIN_LEAGUE));

        mockMvc.perform(post("/api/v1/seasons")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void create_withEndDateBeforeStartDate_returns400() throws Exception {
        String auth = adminAuthHeader();
        String body = """
                {"leagueId": %d, "name": "Obrnuti datumi", "startDate": "2026-12-20", "endDate": "2026-09-19", "playerCount": 100}
                """.formatted(leagueId(auth, MAIN_LEAGUE));

        mockMvc.perform(post("/api/v1/seasons")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_withDuplicateNameInSameLeague_returns409() throws Exception {
        String auth = adminAuthHeader();
        String body = """
                {"leagueId": %d, "name": "%s", "playerCount": 100}
                """.formatted(leagueId(auth, MAIN_LEAGUE), SEEDED_SEASON);

        mockMvc.perform(post("/api/v1/seasons")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void create_withSameNameInAnotherLeague_isAllowed() throws Exception {
        String auth = adminAuthHeader();
        String body = """
                {"leagueId": %d, "name": "%s", "playerCount": 100}
                """.formatted(leagueId(auth, "Floor General 1v1 Liga - Rekreativna divizija"), SEEDED_SEASON);

        mockMvc.perform(post("/api/v1/seasons")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void update_scoringWhileDraft_isPersisted() throws Exception {
        String auth = adminAuthHeader();
        Long draftId = createSeason(auth, "Sezona u pripremi");
        String body = """
                {"name": "Sezona u pripremi", "playerCount": 80, "pointsForWin": 3,
                 "targetScore": 21, "maxScore": 22}
                """;

        MvcResult result = mockMvc.perform(put("/api/v1/seasons/{id}", draftId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        SeasonInfo updated = objectMapper.readValue(result.getResponse().getContentAsString(), SeasonInfo.class);
        assertThat(updated.pointsForWin()).isEqualTo(3);
        assertThat(updated.targetScore()).isEqualTo(21);
        assertThat(updated.maxScore()).isEqualTo(22);
        assertThat(updated.playerCount()).isEqualTo(80);
    }

    @Test
    void update_scoringWhileActive_returns409() throws Exception {
        String auth = adminAuthHeader();
        SeasonResult seeded = searchSeasons(auth, "")[0];
        String body = """
                {"name": "%s", "playerCount": 4, "pointsForWin": 3}
                """.formatted(SEEDED_SEASON);

        mockMvc.perform(put("/api/v1/seasons/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void changeStatus_draftToActive_isAllowedAndActiveToDraftIsNot() throws Exception {
        String auth = adminAuthHeader();
        Long draftId = createSeason(auth, "Sezona za aktiviranje");

        mockMvc.perform(patch("/api/v1/seasons/{id}/status", draftId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/v1/seasons/{id}/status", draftId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DRAFT\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_seasonWithoutStages_removesSeason() throws Exception {
        String auth = adminAuthHeader();
        Long draftId = createSeason(auth, "Sezona za brisanje");

        mockMvc.perform(delete("/api/v1/seasons/{id}", draftId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/seasons/{id}", draftId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_seasonWithStages_returns409() throws Exception {
        String auth = adminAuthHeader();
        SeasonResult seeded = searchSeasons(auth, "")[0];

        mockMvc.perform(delete("/api/v1/seasons/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    private SeasonResult[] searchSeasons(String auth, String queryString) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/seasons" + queryString)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), SeasonResult[].class);
    }

}
