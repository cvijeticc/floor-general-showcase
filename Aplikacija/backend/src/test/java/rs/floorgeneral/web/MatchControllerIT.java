package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.web.dto.court.CourtResult;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.MatchResult;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;

/**
 * UC-ADM-08. Seed ima jedan odigran mec (Marković 11:7 Petrović, 19.09.2026. u 19:00, kolo 1)
 * u grupi "Grupa A" — na njemu se proveravaju pretraga, preklapanje termina i zabrana brisanja.
 */
class MatchControllerIT extends AbstractIntegrationTest {

    private static final String SEEDED_SLOT = "2026-09-19T19:00:00";

    @Test
    void search_byGroup_returnsSeededMatch() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);

        MatchResult[] matches = searchMatches(auth, "?groupId=" + groupId);

        assertThat(matches).hasSize(1);
        assertThat(matches[0].player1Name()).isEqualTo("Marko Marković");
        assertThat(matches[0].scorePlayer1()).isEqualTo(11);
        assertThat(matches[0].status()).isEqualTo(MatchStatus.FINISHED);
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/matches"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void search_byDateRoundAndStatus_filtersResults() throws Exception {
        String auth = adminAuthHeader();

        assertThat(searchMatches(auth, "?date=2026-09-19")).hasSize(1);
        assertThat(searchMatches(auth, "?date=2026-09-20")).isEmpty();
        assertThat(searchMatches(auth, "?round=1")).hasSize(1);
        assertThat(searchMatches(auth, "?round=9")).isEmpty();
        assertThat(searchMatches(auth, "?status=FINISHED")).hasSize(1);
        assertThat(searchMatches(auth, "?status=SCHEDULED")).isEmpty();
    }

    @Test
    void findById_returnsPlayersAndWinner() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = searchMatches(auth, "")[0];

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        MatchInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class);
        assertThat(info.winnerName()).isEqualTo("Marko Marković");
        assertThat(info.groupName()).isEqualTo("Grupa A");
        assertThat(info.stageName()).isEqualTo("Grupna faza");
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/matches/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    /** Odluka D-07 — mec u proslosti sa rezultatom, u jednom pozivu. */
    @Test
    void create_alreadyPlayedMatchInThePast_isStoredAsFinished() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        MatchInfo created = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d,
                 "scheduledStart": "2026-09-20T12:00:00", "scorePlayer1": 7, "scorePlayer2": 11}
                """.formatted(groupId, players.get(0), players.get(2)));

        assertThat(created.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(created.winnerId()).isEqualTo(players.get(2));
        assertThat(created.scheduledStart()).isNotNull();
    }

    @Test
    void create_withoutResult_isScheduled() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        MatchInfo created = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                """.formatted(groupId, players.get(1), players.get(2)));

        assertThat(created.status()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(created.scorePlayer1()).isNull();
        assertThat(created.winnerId()).isNull();
    }

    @Test
    void create_samePlayerTwice_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        Long playerId = seededGroupPlayers(auth, groupId).get(0);

        mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                                """.formatted(groupId, playerId, playerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_withPlayerOutsideGroup_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        Long insider = seededGroupPlayers(auth, groupId).get(0);
        Long outsider = createPlayer(auth, "Stefan", "Stefanović");

        mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                                """.formatted(groupId, insider, outsider)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_whenPlayerAlreadyHasMatchAtThatSlot_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d,
                                 "scheduledStart": "%s"}
                                """.formatted(groupId, players.get(0), players.get(2), SEEDED_SLOT)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_whenCourtIsAlreadyTakenAtThatSlot_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long courtId = anyCourtId(auth);
        Long stageId = createGroupStage(auth, "Sezona sa terenima", 10);
        Long groupId = createGroup(auth, stageId, "Grupa A");
        Long first = createPlayer(auth, "Stefan", "Stefanović");
        Long second = createPlayer(auth, "Luka", "Lukić");
        Long third = createPlayer(auth, "Ivan", "Ivanović");
        Long fourth = createPlayer(auth, "Vuk", "Vukić");
        assign(auth, groupId, first, second, third, fourth);

        createMatch(auth, """
                {"groupId": %d, "roundNumber": 1, "player1Id": %d, "player2Id": %d,
                 "courtId": %d, "scheduledStart": "2026-10-03T10:00:00"}
                """.formatted(groupId, first, second, courtId));

        mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"groupId": %d, "roundNumber": 1, "player1Id": %d, "player2Id": %d,
                                 "courtId": %d, "scheduledStart": "2026-10-03T10:00:00"}
                                """.formatted(groupId, third, fourth, courtId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_withDrawResult_returns400() throws Exception {
        assertResultRejected("""
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d,
                 "scorePlayer1": 10, "scorePlayer2": 10}
                """);
    }

    @Test
    void create_withScoreAboveMaxScore_returns400() throws Exception {
        assertResultRejected("""
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d,
                 "scorePlayer1": 13, "scorePlayer2": 9}
                """);
    }

    /** D-18 — 12:7 je ispravan rezultat: ko vodi 10:7 i pogodi spolja zavrsava na 12. */
    @Test
    void create_withTwelvePoints_isStoredAsFinished() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        MatchInfo created = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d,
                 "scorePlayer1": 12, "scorePlayer2": 7}
                """.formatted(groupId, players.get(0), players.get(2)));

        assertThat(created.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(created.winnerId()).isEqualTo(players.get(0));
    }

    /** D-18 — mec traje osam minuta, pa pobedjuje onaj ko vodi i kad niko nije stigao do 11. */
    @Test
    void create_whenNobodyReachedTargetScore_isStoredAsFinished() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        MatchInfo created = createMatch(auth, """
                {"groupId": %d, "roundNumber": 3, "player1Id": %d, "player2Id": %d,
                 "scorePlayer1": 9, "scorePlayer2": 7}
                """.formatted(groupId, players.get(0), players.get(2)));

        assertThat(created.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(created.winnerId()).isEqualTo(players.get(0));
    }

    @Test
    void update_changesCourtAndSlot() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);
        MatchInfo match = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                """.formatted(groupId, players.get(1), players.get(2)));
        Long courtId = anyCourtId(auth);

        MvcResult result = mockMvc.perform(put("/api/v1/matches/{id}", match.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roundNumber": 3, "courtId": %d, "scheduledStart": "2026-10-04T18:30:00",
                                 "note": "Pomeren termin"}
                                """.formatted(courtId)))
                .andExpect(status().isOk())
                .andReturn();

        MatchInfo updated = objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class);
        assertThat(updated.roundNumber()).isEqualTo(3);
        assertThat(updated.courtId()).isEqualTo(courtId);
        assertThat(updated.note()).isEqualTo("Pomeren termin");
    }

    @Test
    void postpone_setsStatusAndRequiresNote() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);
        MatchInfo match = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d,
                 "scheduledStart": "2026-10-04T18:30:00"}
                """.formatted(groupId, players.get(1), players.get(2)));

        mockMvc.perform(patch("/api/v1/matches/{id}/postpone", match.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledStart\": \"2026-10-11T18:30:00\", \"note\": \"Kisa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("POSTPONED"))
                .andExpect(jsonPath("$.note").value("Kisa"));

        mockMvc.perform(patch("/api/v1/matches/{id}/postpone", match.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledStart\": \"2026-10-11T18:30:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.note").isNotEmpty());
    }

    @Test
    void delete_scheduledMatch_removesIt() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);
        MatchInfo match = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                """.formatted(groupId, players.get(1), players.get(2)));

        mockMvc.perform(delete("/api/v1/matches/{id}", match.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/matches/{id}", match.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_finishedMatch_returns409() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = searchMatches(auth, "")[0];

        mockMvc.perform(delete("/api/v1/matches/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    private void assertResultRejected(String bodyTemplate) throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyTemplate.formatted(groupId, players.get(1), players.get(2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private MatchResult[] searchMatches(String auth, String queryString) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/matches" + queryString)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MatchResult[].class);
    }

    private MatchInfo createMatch(String auth, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class);
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

    private Long anyCourtId(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/courts").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        CourtResult[] courts = objectMapper.readValue(result.getResponse().getContentAsString(), CourtResult[].class);
        return courts[0].id();
    }

    private Long createGroup(String auth, Long stageId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/stages/{id}/groups", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), GroupInfo.class).id();
    }

    private void assign(String auth, Long groupId, Long... playerIds) throws Exception {
        String ids = Arrays.stream(playerIds).map(String::valueOf).reduce((a, b) -> a + ", " + b).orElseThrow();
        mockMvc.perform(post("/api/v1/groups/{id}/players", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\": [%s]}".formatted(ids)))
                .andExpect(status().isOk());
    }

    private Long createPlayer(String auth, String firstName, String lastName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/players")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\": \"%s\", \"lastName\": \"%s\", \"nickname\": \"%s\"}"
                                .formatted(firstName, lastName, firstName)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class).id();
    }
}
