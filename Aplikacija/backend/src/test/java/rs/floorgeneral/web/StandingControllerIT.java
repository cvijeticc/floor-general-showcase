package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;
import rs.floorgeneral.web.dto.standing.StandingResult;

/**
 * UC-ENG-01 + UC-ADM-12. Seed: grupa "Grupa A" sa 3 igraca i jednim odigranim mecem
 * (Marković 11:7 Petrović). Tabela u seedu nije popunjena — puni je preracun.
 */
class StandingControllerIT extends AbstractIntegrationTest {

    @Test
    void standings_beforeAnyRecompute_areEmpty() throws Exception {
        String auth = adminAuthHeader();

        assertThat(standings(auth, seededGroupId(auth))).isEmpty();
    }

    @Test
    void standings_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/groups/{id}/standings", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void standings_unknownGroup_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/groups/{id}/standings", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void recompute_buildsTableFromPlayedMatches() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);

        List<StandingResult> table = recompute(auth, groupId);

        assertThat(table).hasSize(3);
        StandingResult first = table.get(0);
        assertThat(first.playerName()).isEqualTo("Marko Marković");
        assertThat(first.position()).isEqualTo(1);
        assertThat(first.played()).isEqualTo(1);
        assertThat(first.wins()).isEqualTo(1);
        assertThat(first.pointsFor()).isEqualTo(11);
        assertThat(first.pointsAgainst()).isEqualTo(7);
        assertThat(first.pointDifference()).isEqualTo(4);
        assertThat(first.points()).isEqualTo(2);

        StandingResult loser = row(table, "Petar Petrović");
        assertThat(loser.losses()).isEqualTo(1);
        assertThat(loser.points()).isEqualTo(1);

        StandingResult noMatches = row(table, "Jovan Jovanović");
        assertThat(noMatches.played()).isZero();
        assertThat(noMatches.points()).isZero();
    }

    /** BR-12.3 — dva uzastopna preracuna daju identicnu tabelu. */
    @Test
    void recompute_isIdempotent() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);

        List<StandingResult> first = recompute(auth, groupId);
        List<StandingResult> second = recompute(auth, groupId);

        assertThat(second).isEqualTo(first);
        assertThat(standings(auth, groupId)).isEqualTo(first);
    }

    @Test
    void enteringResult_refreshesStandingsAutomatically() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);
        recompute(auth, groupId);

        Long matchId = createScheduledMatch(auth, groupId, players.get(0), players.get(2));
        mockMvc.perform(post("/api/v1/matches/{id}/result", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 11, \"scorePlayer2\": 5}"))
                .andExpect(status().isOk());

        List<StandingResult> table = standings(auth, groupId);
        StandingResult jovanovic = row(table, "Jovan Jovanović");
        assertThat(jovanovic.played()).isEqualTo(1);
        assertThat(jovanovic.wins()).isEqualTo(1);
        assertThat(jovanovic.points()).isEqualTo(2);
        assertThat(jovanovic.position()).isEqualTo(1);
    }

    @Test
    void bulkResults_refreshStandingsOnce() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);
        Long firstMatch = createScheduledMatch(auth, groupId, players.get(0), players.get(1));
        Long secondMatch = createScheduledMatch(auth, groupId, players.get(0), players.get(2));

        mockMvc.perform(put("/api/v1/matches/results")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"results": [
                                    {"matchId": %d, "scorePlayer1": 11, "scorePlayer2": 2},
                                    {"matchId": %d, "scorePlayer1": 11, "scorePlayer2": 3}
                                ]}
                                """.formatted(firstMatch, secondMatch)))
                .andExpect(status().isOk());

        StandingResult jovanovic = row(standings(auth, groupId), "Jovan Jovanović");
        assertThat(jovanovic.played()).isEqualTo(2);
        assertThat(jovanovic.points()).isEqualTo(4);
        assertThat(jovanovic.position()).isEqualTo(1);
    }

    /** BR-10.2 — kaznjeni dobija 0 bodova, protivnik pobedu 11:0. */
    @Test
    void forfeit_refreshesStandingsWithZeroPointsForPunishedPlayer() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);
        Long matchId = createScheduledMatch(auth, groupId, players.get(0), players.get(1));

        mockMvc.perform(post("/api/v1/matches/{id}/forfeit", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": %d, "type": "WALKOVER", "note": "Nije se pojavio"}
                                """.formatted(players.get(0))))
                .andExpect(status().isOk());

        List<StandingResult> table = standings(auth, groupId);
        StandingResult punished = row(table, "Jovan Jovanović");
        assertThat(punished.points()).isZero();
        assertThat(punished.losses()).isEqualTo(1);
        assertThat(punished.pointsAgainst()).isEqualTo(11);
    }

    @Test
    void correction_refreshesStandings() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        recompute(auth, groupId);
        Long seededMatchId = seededMatchId(auth, groupId);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/v1/matches/{id}/result", seededMatchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scorePlayer1": 5, "scorePlayer2": 11, "reason": "Zamenjeni igraci u zapisniku"}
                                """))
                .andExpect(status().isOk());

        List<StandingResult> table = standings(auth, groupId);
        assertThat(row(table, "Petar Petrović").points()).isEqualTo(2);
        assertThat(row(table, "Marko Marković").points()).isEqualTo(1);
        assertThat(table.get(0).playerName()).isEqualTo("Petar Petrović");
    }

    private List<StandingResult> standings(String auth, Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/groups/{id}/standings", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return List.of(objectMapper.readValue(result.getResponse().getContentAsString(), StandingResult[].class));
    }

    private List<StandingResult> recompute(String auth, Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/groups/{id}/standings/recompute", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return List.of(objectMapper.readValue(result.getResponse().getContentAsString(), StandingResult[].class));
    }

    private Long createScheduledMatch(String auth, Long groupId, Long player1, Long player2) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                                """.formatted(groupId, player1, player2)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class).id();
    }

    private Long seededMatchId(String auth, Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/matches?groupId=" + groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    private Long seededGroupId(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stages/{id}/groups", seededStageId(auth))
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    /** Igraci grupe, sortirani po prezimenu: Jovanović, Marković, Petrović. */
    private List<Long> seededGroupPlayers(String auth, Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/players?groupId=" + groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        PlayerResult[] players = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerResult[].class);
        return Arrays.stream(players).map(PlayerResult::id).toList();
    }

    private static StandingResult row(List<StandingResult> table, String playerName) {
        return table.stream()
                .filter(standing -> standing.playerName().equals(playerName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Nema reda za igraca " + playerName));
    }
}
