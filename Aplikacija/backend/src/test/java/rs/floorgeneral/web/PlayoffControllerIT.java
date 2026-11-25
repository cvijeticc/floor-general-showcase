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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.domain.StageType;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.playoff.BracketInfo;
import rs.floorgeneral.web.dto.playoff.RoundInfo;
import rs.floorgeneral.web.dto.playoff.SeriesInfo;
import rs.floorgeneral.web.dto.site.PublicBracket;
import rs.floorgeneral.web.dto.site.PublicMatch;
import rs.floorgeneral.web.dto.site.PublicStatRow;

/**
 * Rok 3 — playoff (BR-08, BR-09): drvo od 4 igraca (polufinale na 2 pobede, finale i mec za
 * 3. mesto na jednu), unos meceva, prolaz u sledece kolo, sluzbeni rezultat, ispravke i „Ponisti".
 * Playoff se dodaje seed sezoni („Test sezona", ACTIVE), da bi bio vidljiv i na javnom sajtu.
 */
class PlayoffControllerIT extends AbstractIntegrationTest {

    private String auth;
    private Long stageId;
    private long ana;
    private long bojan;
    private long ceca;
    private long dule;

    @BeforeEach
    void bracketOfFour() throws Exception {
        auth = adminAuthHeader();
        stageId = createStage(auth, seededSeasonId(auth), """
                {"name": "Playoff", "type": "KNOCKOUT", "orderNo": 2, "bracketSize": 4}
                """).id();
        ana = createPlayer("Ana", "Anić", "Ana");
        bojan = createPlayer("Bojan", "Bojić", "Boki");
        ceca = createPlayer("Ceca", "Cecić", "Ceca");
        dule = createPlayer("Dušan", "Dulić", "Dule");

        BracketInfo bracket = read(perform(post("/api/v1/stages/{id}/bracket", stageId), """
                {"firstRound": [
                  {"player1Id": %d, "player2Id": %d, "seed1": "1. Gr. A", "seed2": "4. Gr. A"},
                  {"player1Id": %d, "player2Id": %d, "seed1": "2. Gr. A", "seed2": "3. Gr. A"}
                ]}
                """.formatted(ana, bojan, ceca, dule)).andExpect(status().isCreated()), BracketInfo.class);

        // Polufinale na dve pobede, finale do 16 (najvise 17) — kao Final Four lige.
        perform(put("/api/v1/stage-rounds/{id}", bracket.rounds().get(0).id()),
                """
                {"name": "Polufinale", "winsRequired": 2}
                """).andExpect(status().isOk());
        perform(put("/api/v1/stage-rounds/{id}", bracket.rounds().get(1).id()),
                """
                {"name": "Finale", "winsRequired": 1, "targetScore": 16, "maxScore": 17, "durationMinutes": 15}
                """).andExpect(status().isOk());
    }

    @Test
    void createBracket_buildsRoundsSeriesAndThirdPlace() throws Exception {
        BracketInfo bracket = bracket();

        assertThat(bracket.bracketSize()).isEqualTo(4);
        assertThat(bracket.rounds()).extracting(RoundInfo::name).containsExactly("Polufinale", "Finale");
        RoundInfo semis = bracket.rounds().get(0);
        assertThat(semis.winsRequired()).isEqualTo(2);
        assertThat(semis.ownFormat()).isFalse();
        assertThat(semis.maxScore()).isEqualTo(12);
        assertThat(semis.series()).hasSize(2);
        assertThat(semis.series().get(0).player1().nickname()).isEqualTo("Ana");
        assertThat(semis.series().get(0).seed2()).isEqualTo("4. Gr. A");
        assertThat(semis.series().get(0).status()).isEqualTo("READY");
        assertThat(semis.series()).extracting(SeriesInfo::number).containsExactly(1, 2);

        RoundInfo finals = bracket.rounds().get(1);
        assertThat(finals.maxScore()).isEqualTo(17);
        assertThat(finals.ownFormat()).isTrue();
        assertThat(finals.series()).extracting(SeriesInfo::thirdPlace).containsExactly(false, true);
        assertThat(finals.series()).extracting(SeriesInfo::status).containsOnly("PENDING");
        assertThat(finals.series()).extracting(SeriesInfo::number).containsExactly(3, 4);

        perform(post("/api/v1/stages/{id}/bracket", stageId), "{}").andExpect(status().isConflict());
    }

    @Test
    void seriesOfTwoWins_winnerAdvancesAndThirdGameIsRefused() throws Exception {
        SeriesInfo first = semi(1);

        SeriesInfo afterOne = series(addGame(first.id(), 11, 7).andExpect(status().isCreated()));
        assertThat(afterOne.status()).isEqualTo("IN_PROGRESS");
        assertThat(afterOne.wins1()).isEqualTo(1);
        assertThat(finalSeries().player1()).isNull();

        SeriesInfo afterTwo = series(addGame(first.id(), 12, 10).andExpect(status().isCreated()));
        assertThat(afterTwo.status()).isEqualTo("FINISHED");
        assertThat(afterTwo.winnerId()).isEqualTo(ana);
        assertThat(afterTwo.games()).extracting(g -> g.gameNumber()).containsExactly(1, 2);

        assertThat(finalSeries().player1().id()).isEqualTo(ana);
        assertThat(finalSeries().player2()).isNull();

        addGame(first.id(), 11, 0).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("vec resena")));
        // Finale jos nema oba igraca.
        addGame(finalSeries().id(), 16, 10).andExpect(status().isConflict());
    }

    @Test
    void walkover_sendsWinnerToFinal_andLosersOfSemisMeetForThirdPlace() throws Exception {
        addGame(semi(1).id(), 11, 7);
        addGame(semi(1).id(), 11, 9);

        SeriesInfo walkover = series(perform(post("/api/v1/series/{id}/walkover", semi(2).id()),
                """
                {"winnerId": %d, "reason": "Ceca se nije pojavila"}
                """.formatted(dule)).andExpect(status().isOk()));
        assertThat(walkover.walkover()).isTrue();
        assertThat(walkover.winnerId()).isEqualTo(dule);

        SeriesInfo fin = finalSeries();
        assertThat(fin.player1().id()).isEqualTo(ana);
        assertThat(fin.player2().id()).isEqualTo(dule);
        SeriesInfo third = thirdPlace();
        assertThat(third.player1().id()).isEqualTo(bojan);
        assertThat(third.player2().id()).isEqualTo(ceca);

        // Format finala: do 16, najvise 17 — 18 se odbija, 17:15 prolazi i resava finale.
        addGame(fin.id(), 18, 15).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("17")));
        SeriesInfo champion = series(addGame(fin.id(), 15, 17).andExpect(status().isCreated()));
        assertThat(champion.winnerId()).isEqualTo(dule);
        assertThat(champion.status()).isEqualTo("FINISHED");
    }

    @Test
    void correctionThatChangesWinner_isRefusedOnceNextSeriesStarted_thenAllowed() throws Exception {
        SeriesInfo first = semi(1);
        addGame(first.id(), 11, 7);
        SeriesInfo decided = series(addGame(first.id(), 11, 9));
        addGame(semi(2).id(), 11, 3);
        addGame(semi(2).id(), 11, 4);
        SeriesInfo fin = finalSeries();
        series(addGame(fin.id(), 16, 12).andExpect(status().isCreated()));

        Long secondGame = decided.games().get(1).matchId();
        perform(put("/api/v1/series/{id}/games/{matchId}", first.id(), secondGame),
                """
                {"score1": 9, "score2": 11, "reason": "pogresno upisano"}
                """).andExpect(status().isConflict());

        // Kad se mec finala obrise, ispravka prolazi: serija je 1:1, Ana vise nije u finalu.
        perform(delete("/api/v1/series/{id}/games/{matchId}", fin.id(), finalSeries().games().get(0).matchId()), null)
                .andExpect(status().isNoContent());
        SeriesInfo corrected = series(perform(put("/api/v1/series/{id}/games/{matchId}", first.id(), secondGame),
                """
                {"score1": 9, "score2": 11}
                """).andExpect(status().isOk()));
        assertThat(corrected.winnerId()).isNull();
        assertThat(corrected.wins1()).isEqualTo(1);
        assertThat(corrected.wins2()).isEqualTo(1);
        assertThat(finalSeries().player1()).isNull();
        assertThat(thirdPlace().player1()).isNull();

        // „Ponisti" vraca i rezultat i drvo.
        mockMvc.perform(post("/api/v1/undo").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isOk());
        assertThat(semi(1).winnerId()).isEqualTo(ana);
        assertThat(finalSeries().player1().id()).isEqualTo(ana);
        assertThat(thirdPlace().player1().id()).isEqualTo(bojan);
    }

    @Test
    void onlyLastGameCanBeDeleted_andUndoRestoresIt() throws Exception {
        SeriesInfo first = semi(1);
        addGame(first.id(), 11, 7);
        SeriesInfo two = series(addGame(first.id(), 7, 11));

        perform(delete("/api/v1/series/{id}/games/{matchId}", first.id(), two.games().get(0).matchId()), null)
                .andExpect(status().isConflict());
        perform(delete("/api/v1/series/{id}/games/{matchId}", first.id(), two.games().get(1).matchId()), null)
                .andExpect(status().isNoContent());
        assertThat(semi(1).games()).hasSize(1);

        mockMvc.perform(post("/api/v1/undo").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isOk());
        assertThat(semi(1).games()).extracting(g -> g.matchId()).containsExactly(
                two.games().get(0).matchId(), two.games().get(1).matchId());
    }

    @Test
    void seriesGame_cannotBeChangedThroughGeneralMatchEndpoints() throws Exception {
        SeriesInfo one = series(addGame(semi(1).id(), 11, 7));
        Long matchId = one.games().get(0).matchId();

        perform(patch("/api/v1/matches/{id}/result", matchId),
                """
                {"scorePlayer1": 7, "scorePlayer2": 11, "reason": "test"}
                """).andExpect(status().isConflict());
        perform(delete("/api/v1/matches/{id}", matchId), null).andExpect(status().isConflict());
    }

    @Test
    void setPlayers_refusedAfterStart_andPlayerCannotBeTwiceInRound() throws Exception {
        SeriesInfo first = semi(1);
        perform(put("/api/v1/series/{id}/players", semi(2).id()),
                """
                {"player1Id": %d, "player2Id": %d}
                """.formatted(ana, dule)).andExpect(status().isConflict());

        addGame(first.id(), 11, 7);
        perform(put("/api/v1/series/{id}/players", first.id()),
                """
                {"player1Id": %d, "player2Id": %d}
                """.formatted(ana, dule)).andExpect(status().isConflict());
    }

    @Test
    void publicPlayoff_showsBracketWithGames_andMatchesKnowTheirRound() throws Exception {
        perform(patch("/api/v1/stages/{id}/status", stageId), """
                {"status": "ACTIVE"}
                """).andExpect(status().isOk());
        addGame(semi(1).id(), 11, 7);

        MvcResult result = mockMvc.perform(get("/api/v1/public/playoff")).andExpect(status().isOk()).andReturn();
        String json = result.getResponse().getContentAsString();
        PublicBracket bracket = objectMapper.readValue(json, PublicBracket.class);

        assertThat(bracket.rounds()).extracting(PublicBracket.PublicRound::name).containsExactly("Polufinale", "Finale");
        PublicBracket.PublicSeries first = bracket.rounds().get(0).series().get(0);
        assertThat(first.player1().nickname()).isEqualTo("Ana");
        assertThat(first.games()).hasSize(1);
        assertThat(first.games().get(0).score1()).isEqualTo(11);
        assertThat(json).doesNotContain("\"note\"");

        MvcResult matches = mockMvc.perform(get("/api/v1/public/matches")
                        .param("seasonId", seededSeasonId(auth).toString()).param("played", "true"))
                .andExpect(status().isOk()).andReturn();
        PublicMatch playoffGame = Arrays.stream(objectMapper.readValue(matches.getResponse().getContentAsString(),
                        PublicMatch[].class))
                .filter(m -> m.seriesId() != null)
                .findFirst()
                .orElseThrow();
        assertThat(playoffGame.stageType()).isEqualTo(StageType.KNOCKOUT);
        assertThat(playoffGame.roundName()).isEqualTo("Polufinale");
        assertThat(playoffGame.gameNumber()).isEqualTo(1);
        assertThat(playoffGame.groupId()).isNull();

        MvcResult stats = mockMvc.perform(get("/api/v1/public/stats").param("phase", "KNOCKOUT"))
                .andExpect(status().isOk()).andReturn();
        PublicStatRow[] rows = objectMapper.readValue(stats.getResponse().getContentAsString(), PublicStatRow[].class);
        assertThat(rows).extracting(row -> row.player().nickname()).containsExactly("Ana", "Boki");
        assertThat(rows[0].pointsFor()).isEqualTo(11);
        assertThat(rows[0].averageFor()).isEqualTo(11.0);
    }

    @Test
    void publicPlayoff_isHiddenWhileStageIsDraft() throws Exception {
        mockMvc.perform(get("/api/v1/public/playoff")).andExpect(status().isNotFound());
    }

    @Test
    void bracket_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stages/{id}/bracket", stageId)).andExpect(status().isUnauthorized());
    }

    /* ------------------------------------------------------------------ pomocne */

    private long createPlayer(String firstName, String lastName, String nickname) throws Exception {
        MvcResult result = perform(post("/api/v1/players"), """
                {"firstName": "%s", "lastName": "%s", "nickname": "%s"}
                """.formatted(firstName, lastName, nickname)).andExpect(status().isCreated()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class).id();
    }

    private ResultActions addGame(Long seriesId, int score1, int score2) throws Exception {
        return perform(post("/api/v1/series/{id}/games", seriesId), """
                {"score1": %d, "score2": %d}
                """.formatted(score1, score2));
    }

    private ResultActions perform(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                                  String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, auth);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private BracketInfo bracket() throws Exception {
        return read(perform(get("/api/v1/stages/{id}/bracket", stageId), null).andExpect(status().isOk()), BracketInfo.class);
    }

    private SeriesInfo semi(int position) throws Exception {
        return bracket().rounds().get(0).series().get(position - 1);
    }

    private SeriesInfo finalSeries() throws Exception {
        return bracket().rounds().get(1).series().get(0);
    }

    private SeriesInfo thirdPlace() throws Exception {
        return bracket().rounds().get(1).series().get(1);
    }

    private SeriesInfo series(ResultActions actions) throws Exception {
        return read(actions, SeriesInfo.class);
    }

    private <T> T read(ResultActions actions, Class<T> type) throws Exception {
        return objectMapper.readValue(actions.andReturn().getResponse().getContentAsString(), type);
    }
}
