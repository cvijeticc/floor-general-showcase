package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import rs.floorgeneral.web.dto.match.BulkResultsInfo;
import rs.floorgeneral.web.dto.match.CorrectionInfo;
import rs.floorgeneral.web.dto.match.ForfeitInfo;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.match.MatchResult;
import rs.floorgeneral.web.dto.player.PlayerResult;

/**
 * UC-ADM-09. Seed ima jedan vec odigran mec (Marković 11:7 Petrović) na kome se proverava
 * da se rezultat ne upisuje dvaput, i tri igraca od kojih se prave novi, jos neodigrani mecevi.
 */
class MatchResultControllerIT extends AbstractIntegrationTest {

    @Test
    void enterResult_setsWinnerAndFinishesMatch() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{id}/result", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 7, \"scorePlayer2\": 11}"))
                .andExpect(status().isOk())
                .andReturn();

        MatchInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class);
        assertThat(info.status()).isEqualTo(MatchStatus.FINISHED);
        assertThat(info.scorePlayer2()).isEqualTo(11);
        assertThat(info.winnerId()).isEqualTo(info.player2Id());
    }

    @Test
    void enterResult_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/matches/{id}/result", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 11, \"scorePlayer2\": 7}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enterResult_onMatchThatAlreadyHasResult_returns409() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = seededFinishedMatch(auth);

        mockMvc.perform(post("/api/v1/matches/{id}/result", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 11, \"scorePlayer2\": 2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void enterResult_withDraw_returns400() throws Exception {
        assertRejected("{\"scorePlayer1\": 10, \"scorePlayer2\": 10}");
    }

    @Test
    void enterResult_withScoreAboveMaxScore_returns400() throws Exception {
        assertRejected("{\"scorePlayer1\": 13, \"scorePlayer2\": 9}");
    }

    /** D-18 — 12:9 i 9:7 su ispravni rezultati: mec traje osam minuta, a sut spolja vredi dva poena. */
    @Test
    void enterResult_twelvePointsAndMatchDecidedOnTime_areAccepted() throws Exception {
        String auth = adminAuthHeader();

        Long overtime = createScheduledMatch(auth, 4, 5);
        mockMvc.perform(post("/api/v1/matches/{id}/result", overtime)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 12, \"scorePlayer2\": 9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        Long onTime = createScheduledMatch(auth, 5, 6);
        mockMvc.perform(post("/api/v1/matches/{id}/result", onTime)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 9, \"scorePlayer2\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void enterResult_withMissingScore_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);

        mockMvc.perform(post("/api/v1/matches/{id}/result", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.scorePlayer2").isNotEmpty());
    }

    @Test
    void enterResult_unknownMatch_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(post("/api/v1/matches/{id}/result", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 11, \"scorePlayer2\": 7}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void enterResults_appliesWholeRoundInOneCall() throws Exception {
        String auth = adminAuthHeader();
        Long firstMatch = createScheduledMatch(auth, 1, 2);
        Long secondMatch = createScheduledMatch(auth, 2, 3);

        BulkResultsInfo info = bulk(auth, """
                {"results": [
                    {"matchId": %d, "scorePlayer1": 11, "scorePlayer2": 4},
                    {"matchId": %d, "scorePlayer1": 6, "scorePlayer2": 11}
                ]}
                """.formatted(firstMatch, secondMatch));

        assertThat(info.appliedCount()).isEqualTo(2);
        assertThat(info.rejectedCount()).isZero();
        assertThat(matchStatus(auth, firstMatch)).isEqualTo(MatchStatus.FINISHED);
        assertThat(matchStatus(auth, secondMatch)).isEqualTo(MatchStatus.FINISHED);
    }

    /** Glavno pravilo masovnog unosa (UC-ADM-09): jedan los red ne obara ceo unos. */
    @Test
    void enterResults_badRowIsRejectedAndGoodRowIsStored() throws Exception {
        String auth = adminAuthHeader();
        Long goodMatch = createScheduledMatch(auth, 1, 2);
        Long badMatch = createScheduledMatch(auth, 2, 3);

        BulkResultsInfo info = bulk(auth, """
                {"results": [
                    {"matchId": %d, "scorePlayer1": 11, "scorePlayer2": 4},
                    {"matchId": %d, "scorePlayer1": 9, "scorePlayer2": 9}
                ]}
                """.formatted(goodMatch, badMatch));

        assertThat(info.appliedCount()).isEqualTo(1);
        assertThat(info.rejectedCount()).isEqualTo(1);
        assertThat(info.rejected().get(0).matchId()).isEqualTo(badMatch);
        assertThat(info.rejected().get(0).reason()).isNotBlank();

        assertThat(matchStatus(auth, goodMatch)).isEqualTo(MatchStatus.FINISHED);
        assertThat(matchStatus(auth, badMatch)).isEqualTo(MatchStatus.SCHEDULED);
    }

    @Test
    void enterResults_unknownMatchIsRejectedWithoutFailingTheCall() throws Exception {
        String auth = adminAuthHeader();

        BulkResultsInfo info = bulk(auth, """
                {"results": [{"matchId": 999999, "scorePlayer1": 11, "scorePlayer2": 4}]}
                """);

        assertThat(info.appliedCount()).isZero();
        assertThat(info.rejected().get(0).reason()).contains("ne postoji");
    }

    @Test
    void enterResults_alreadyFinishedMatchIsRejected() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = seededFinishedMatch(auth);

        BulkResultsInfo info = bulk(auth, """
                {"results": [{"matchId": %d, "scorePlayer1": 11, "scorePlayer2": 4}]}
                """.formatted(seeded.id()));

        assertThat(info.rejectedCount()).isEqualTo(1);
        assertThat(info.rejected().get(0).reason()).contains("vec ima rezultat");
    }

    @Test
    void enterResults_withEmptyList_returns400() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(put("/api/v1/matches/results")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"results\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.results").isNotEmpty());
    }

    // ---------- UC-ADM-11: korekcija rezultata ----------

    @Test
    void correctResult_changesStoredResultAndKeepsReason() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = seededFinishedMatch(auth);

        MvcResult result = mockMvc.perform(patch("/api/v1/matches/{id}/result", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scorePlayer1": 9, "scorePlayer2": 11, "reason": "Zapisnicar je zamenio igrace"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        CorrectionInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), CorrectionInfo.class);
        assertThat(info.previousResult()).contains("11:7");
        assertThat(info.match().scorePlayer2()).isEqualTo(11);
        assertThat(info.match().winnerId()).isEqualTo(info.match().player2Id());
        assertThat(info.match().note()).contains("Zapisnicar je zamenio igrace");

        MatchInfo persisted = matchInfo(auth, seeded.id());
        assertThat(persisted.scorePlayer1()).isEqualTo(9);
    }

    @Test
    void correctResult_withoutReason_returns400() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = seededFinishedMatch(auth);

        mockMvc.perform(patch("/api/v1/matches/{id}/result", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scorePlayer1\": 9, \"scorePlayer2\": 11}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").isNotEmpty());
    }

    @Test
    void correctResult_onMatchWithoutResult_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);

        mockMvc.perform(patch("/api/v1/matches/{id}/result", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scorePlayer1": 11, "scorePlayer2": 3, "reason": "Greska"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void correctResult_withInvalidScore_returns400() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = seededFinishedMatch(auth);

        mockMvc.perform(patch("/api/v1/matches/{id}/result", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scorePlayer1": 9, "scorePlayer2": 9, "reason": "Greska"}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(matchInfo(auth, seeded.id()).scorePlayer1()).isEqualTo(11);
    }

    // ---------- UC-ADM-10: sluzbeni rezultat ----------

    @Test
    void applyForfeit_walkover_writesElevenToZeroForTheOpponent() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);
        MatchInfo before = matchInfo(auth, matchId);

        MvcResult result = mockMvc.perform(post("/api/v1/matches/{id}/forfeit", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": %d, "type": "WALKOVER", "note": "Nije se pojavio na terenu"}
                                """.formatted(before.player1Id())))
                .andExpect(status().isOk())
                .andReturn();

        ForfeitInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), ForfeitInfo.class);
        assertThat(info.match().status()).isEqualTo(MatchStatus.WALKOVER);
        assertThat(info.match().scorePlayer1()).isZero();
        assertThat(info.match().scorePlayer2()).isEqualTo(11);
        assertThat(info.match().winnerId()).isEqualTo(before.player2Id());
        assertThat(info.match().note()).isEqualTo("Nije se pojavio na terenu");
        assertThat(info.warning()).isNull();
    }

    @Test
    void applyForfeit_surrender_setsForfeitStatus() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);
        MatchInfo before = matchInfo(auth, matchId);

        mockMvc.perform(post("/api/v1/matches/{id}/forfeit", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": %d, "type": "FORFEIT", "note": "Predao zbog povrede"}
                                """.formatted(before.player2Id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.status").value("FORFEIT"))
                .andExpect(jsonPath("$.match.scorePlayer1").value(11))
                .andExpect(jsonPath("$.match.scorePlayer2").value(0));
    }

    /** BR-10.4 — na drugom nedolasku API vraca upozorenje, ali igraca ne iskljucuje. */
    @Test
    void applyForfeit_secondWalkover_returnsWarning() throws Exception {
        String auth = adminAuthHeader();
        Long firstMatch = createScheduledMatch(auth, 1, 2);
        // Drugi mec mora da ukljucuje istog igraca, inace se nedolasci ne sabiraju.
        Long secondMatch = createScheduledMatch(auth, 0, 1);
        Long punished = matchInfo(auth, firstMatch).player1Id();

        forfeit(auth, firstMatch, punished, "Prvi nedolazak");

        MatchInfo second = matchInfo(auth, secondMatch);
        Long punishedInSecond = second.player1Id().equals(punished) ? second.player1Id() : second.player2Id();
        ForfeitInfo info = forfeit(auth, secondMatch, punishedInSecond, "Drugi nedolazak");

        assertThat(info.warning()).contains("iskljucenje");
    }

    /** D-23 — obojica kaznjena: 0:0 iz parametara sezone i bez pobednika. */
    @Test
    void applyForfeit_doubleForfeit_writesZeroZeroWithoutWinner() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);

        mockMvc.perform(post("/api/v1/matches/{id}/forfeit", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "DOUBLE_FORFEIT", "note": "Nijedan se nije pojavio"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.match.status").value("DOUBLE_FORFEIT"))
                .andExpect(jsonPath("$.match.scorePlayer1").value(0))
                .andExpect(jsonPath("$.match.scorePlayer2").value(0))
                .andExpect(jsonPath("$.match.winnerId").isEmpty())
                .andExpect(jsonPath("$.warning").isEmpty());
    }

    @Test
    void applyForfeit_withoutNote_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);
        MatchInfo before = matchInfo(auth, matchId);

        mockMvc.perform(post("/api/v1/matches/{id}/forfeit", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": %d, "type": "WALKOVER", "note": "  "}
                                """.formatted(before.player1Id())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.note").isNotEmpty());
    }

    @Test
    void applyForfeit_withPlayerOutsideMatch_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);

        mockMvc.perform(post("/api/v1/matches/{id}/forfeit", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": 999999, "type": "WALKOVER", "note": "Nije dosao"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void applyForfeit_onMatchThatAlreadyHasResult_returns409() throws Exception {
        String auth = adminAuthHeader();
        MatchResult seeded = seededFinishedMatch(auth);

        mockMvc.perform(post("/api/v1/matches/{id}/forfeit", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": %d, "type": "WALKOVER", "note": "Nije dosao"}
                                """.formatted(seeded.player1Id())))
                .andExpect(status().isConflict());
    }

    private ForfeitInfo forfeit(String auth, Long matchId, Long losingPlayerId, String note) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matches/{id}/forfeit", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": %d, "type": "WALKOVER", "note": "%s"}
                                """.formatted(losingPlayerId, note)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), ForfeitInfo.class);
    }

    private MatchInfo matchInfo(String auth, Long matchId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/matches/{id}", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class);
    }

    private void assertRejected(String body) throws Exception {
        String auth = adminAuthHeader();
        Long matchId = createScheduledMatch(auth, 1, 2);

        mockMvc.perform(post("/api/v1/matches/{id}/result", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(matchStatus(auth, matchId)).isEqualTo(MatchStatus.SCHEDULED);
    }

    private BulkResultsInfo bulk(String auth, String body) throws Exception {
        MvcResult result = mockMvc.perform(put("/api/v1/matches/results")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), BulkResultsInfo.class);
    }

    /** Pravi neodigran mec izmedju dva seed igraca; round razdvaja meceve medjusobno. */
    private Long createScheduledMatch(String auth, int firstPlayerIndex, int secondPlayerIndex) throws Exception {
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);
        Long player1 = players.get(firstPlayerIndex % players.size());
        Long player2 = players.get(secondPlayerIndex % players.size());

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

    private MatchStatus matchStatus(String auth, Long matchId) throws Exception {
        return matchInfo(auth, matchId).status();
    }

    private MatchResult seededFinishedMatch(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/matches?status=FINISHED")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        MatchResult[] matches = objectMapper.readValue(result.getResponse().getContentAsString(), MatchResult[].class);
        return matches[0];
    }

    private Long seededGroupId(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stages/{id}/groups", seededStageId(auth))
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();
    }

    private List<Long> seededGroupPlayers(String auth, Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/players?groupId=" + groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        PlayerResult[] players = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerResult[].class);
        return Arrays.stream(players).map(PlayerResult::id).toList();
    }
}
