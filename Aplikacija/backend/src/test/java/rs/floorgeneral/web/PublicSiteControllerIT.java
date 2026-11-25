package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.web.dto.site.PublicMatch;
import rs.floorgeneral.web.dto.site.PublicSeason;
import rs.floorgeneral.web.dto.site.PublicStandingRow;

/**
 * Javni tab „Rezultati" (UC-PUB-02/04/05): radi bez tokena i ne odaje licne podatke igraca.
 * Seed: db/test/V6 — „Test sezona" (ACTIVE), „Grupa A", jedan odigran mec Mare–Pera 11:7.
 */
class PublicSiteControllerIT extends AbstractIntegrationTest {

    @Test
    void season_withoutToken_returnsActiveSeasonWithGroups() throws Exception {
        PublicSeason season = currentSeason();

        assertThat(season.name()).isEqualTo(SEEDED_SEASON);
        assertThat(season.leagueName()).isEqualTo(MAIN_LEAGUE);
        assertThat(season.groups()).extracting(PublicSeason.Group::name).containsExactly("Grupa A");
        assertThat(season.groups().get(0).directAdvance()).isEqualTo(2);
    }

    @Test
    void playedMatches_containNicknamesButNoPhoneNumbers() throws Exception {
        PublicSeason season = currentSeason();

        MvcResult result = mockMvc.perform(get("/api/v1/public/matches")
                        .param("seasonId", season.id().toString())
                        .param("played", "true"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        PublicMatch[] matches = objectMapper.readValue(json, PublicMatch[].class);
        assertThat(matches).hasSize(1);
        assertThat(matches[0].player1().nickname()).isEqualTo("Mare");
        assertThat(matches[0].scorePlayer1()).isEqualTo(11);
        assertThat(matches[0].scorePlayer2()).isEqualTo(7);
        assertThat(matches[0].status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(json).doesNotContain("+381601111111").doesNotContain("6500din");
    }

    @Test
    void upcomingMatches_excludePlayedOnes() throws Exception {
        PublicSeason season = currentSeason();

        MvcResult result = mockMvc.perform(get("/api/v1/public/matches")
                        .param("seasonId", season.id().toString())
                        .param("played", "false"))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(objectMapper.readValue(result.getResponse().getContentAsString(), PublicMatch[].class)).isEmpty();
    }

    @Test
    void standings_withoutToken_returnRowsAfterRecompute() throws Exception {
        Long groupId = currentSeason().groups().get(0).id();
        mockMvc.perform(post("/api/v1/groups/{id}/standings/recompute", groupId)
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader()))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/public/groups/{id}/standings", groupId))
                .andExpect(status().isOk())
                .andReturn();

        PublicStandingRow[] rows = objectMapper.readValue(result.getResponse().getContentAsString(),
                PublicStandingRow[].class);
        assertThat(rows).isNotEmpty();
        assertThat(rows[0].position()).isEqualTo(1);
        assertThat(rows[0].player().nickname()).isEqualTo("Mare");
        assertThat(rows[0].wins()).isEqualTo(1);
    }

    @Test
    void standings_unknownGroup_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/groups/{id}/standings", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminRoutes_stayClosedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/matches")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/applications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/export/xlsx")).andExpect(status().isUnauthorized());
    }

    private PublicSeason currentSeason() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/public/season"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PublicSeason.class);
    }
}
