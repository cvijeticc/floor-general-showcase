package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import rs.floorgeneral.domain.MatchStatus;
import rs.floorgeneral.web.dto.match.MatchInfo;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;
import rs.floorgeneral.web.dto.standing.StandingResult;
import rs.floorgeneral.web.dto.undo.UndoEntryResult;

/**
 * UC-ADM-23 — ponistavanje admin izmena.
 * <p>
 * Test namerno ide kroz pravu bazu (Testcontainers): ceo postupak i pociva na tome da se red
 * vrati sa <b>istim</b> primarnim kljucem, sto se na lazima ne bi ni videlo.
 */
class UndoControllerIT extends AbstractIntegrationTest {

    @Test
    void undo_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/undo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void last_whenNothingWasChanged_returns204() throws Exception {
        mockMvc.perform(get("/api/v1/undo/last").header(HttpHeaders.AUTHORIZATION, adminAuthHeader()))
                .andExpect(status().isNoContent());
    }

    @Test
    void undo_whenNothingToUndo_returns409() throws Exception {
        mockMvc.perform(post("/api/v1/undo").header(HttpHeaders.AUTHORIZATION, adminAuthHeader()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /** Glavni slucaj iz zahteva: „ako slucajno obrisem igraca, kliknem Ponisti i vrati se". */
    @Test
    void undo_afterPlayerDelete_restoresPlayerWithSameIdAndData() throws Exception {
        String auth = adminAuthHeader();
        Long playerId = createFullPlayer(auth);

        mockMvc.perform(delete("/api/v1/players/{id}", playerId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/players/{id}", playerId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());

        undo(auth);

        MvcResult result = mockMvc.perform(get("/api/v1/players/{id}", playerId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        PlayerInfo restored = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class);
        assertThat(restored.id()).isEqualTo(playerId);
        assertThat(restored.nickname()).isEqualTo("Cane");
        assertThat(restored.birthYear()).isEqualTo(1994);
        assertThat(restored.heightCm()).isEqualTo(191);
        assertThat(restored.phone()).isEqualTo("0601234567");
        assertThat(restored.note()).isEqualTo("Ne moze subotom");
    }

    @Test
    void undo_afterPlayerCreate_removesPlayer() throws Exception {
        String auth = adminAuthHeader();
        Long playerId = createFullPlayer(auth);

        undo(auth);

        mockMvc.perform(get("/api/v1/players/{id}", playerId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void undo_afterPlayerUpdate_restoresPreviousValues() throws Exception {
        String auth = adminAuthHeader();
        Long playerId = createFullPlayer(auth);

        mockMvc.perform(put("/api/v1/players/{id}", playerId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Promenjen", "lastName": "Promenjenovic", "nickname": "Novi"}
                                """))
                .andExpect(status().isOk());

        undo(auth);

        MvcResult result = mockMvc.perform(get("/api/v1/players/{id}", playerId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        PlayerInfo restored = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class);
        assertThat(restored.nickname()).isEqualTo("Cane");
        assertThat(restored.lastName()).isEqualTo("Canovic");
        // Polja koja PUT nije ni poslao takodje moraju da se vrate — vraca se ceo red, ne samo izmenjeno.
        assertThat(restored.heightCm()).isEqualTo(191);
    }

    @Test
    void undo_afterResultEntry_clearsResultAndRecomputesStandings() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        MatchInfo match = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                """.formatted(groupId, players.get(0), players.get(2)));

        mockMvc.perform(post("/api/v1/matches/{id}/result", match.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scorePlayer1": 11, "scorePlayer2": 4}
                                """))
                .andExpect(status().isOk());

        int playedBeforeUndo = playedGamesOf(auth, groupId, players.get(0));

        undo(auth);

        MvcResult result = mockMvc.perform(get("/api/v1/matches/{id}", match.id())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        MatchInfo reverted = objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class);
        assertThat(reverted.status()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(reverted.scorePlayer1()).isNull();
        assertThat(reverted.winnerId()).isNull();

        // Tabela grupe je izvedena — posle ponistavanja mec vise ne sme da se broji (BR-12.3).
        assertThat(playedGamesOf(auth, groupId, players.get(0))).isEqualTo(playedBeforeUndo - 1);
    }

    /** Masovni unos je jedan zapis: jedno „Ponisti" vraca celo kolo, ne mec po mec. */
    @Test
    void undo_afterBulkEntry_revertsWholeRoundAtOnce() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        MatchInfo first = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                """.formatted(groupId, players.get(0), players.get(1)));
        MatchInfo second = createMatch(auth, """
                {"groupId": %d, "roundNumber": 3, "player1Id": %d, "player2Id": %d}
                """.formatted(groupId, players.get(0), players.get(2)));

        mockMvc.perform(put("/api/v1/matches/results")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"results": [
                                  {"matchId": %d, "scorePlayer1": 11, "scorePlayer2": 3},
                                  {"matchId": %d, "scorePlayer1": 6, "scorePlayer2": 11}
                                ]}
                                """.formatted(first.id(), second.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedCount").value(2));

        undo(auth);

        assertThat(statusOf(auth, first.id())).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(statusOf(auth, second.id())).isEqualTo(MatchStatus.SCHEDULED);
    }

    @Test
    void undo_afterForfeit_clearsOfficialResult() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        List<Long> players = seededGroupPlayers(auth, groupId);

        MatchInfo match = createMatch(auth, """
                {"groupId": %d, "roundNumber": 2, "player1Id": %d, "player2Id": %d}
                """.formatted(groupId, players.get(0), players.get(1)));

        mockMvc.perform(post("/api/v1/matches/{id}/forfeit", match.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"losingPlayerId": %d, "type": "WALKOVER", "note": "Nije se pojavio"}
                                """.formatted(players.get(1))))
                .andExpect(status().isOk());

        undo(auth);

        assertThat(statusOf(auth, match.id())).isEqualTo(MatchStatus.SCHEDULED);
    }

    /** Uklanjanje igraca iz grupe je brisanje reda u group_entries — mora da se vrati isto. */
    @Test
    void undo_afterRemovingPlayerFromGroup_putsHimBack() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);
        Long withoutMatches = playerWithoutMatches(auth, groupId);

        mockMvc.perform(delete("/api/v1/groups/{groupId}/players/{playerId}", groupId, withoutMatches)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
        assertThat(seededGroupPlayers(auth, groupId)).doesNotContain(withoutMatches);

        undo(auth);

        assertThat(seededGroupPlayers(auth, groupId)).contains(withoutMatches);
    }

    /** Ctrl+Z dvaput: stek ide korak dublje, od najnovije izmene ka starijoj. */
    @Test
    void undo_twice_goesOneStepDeeper() throws Exception {
        String auth = adminAuthHeader();
        Long first = createPlayer(auth, "Prvi", "Prvic");
        Long second = createPlayer(auth, "Drugi", "Drugic");

        undo(auth);
        mockMvc.perform(get("/api/v1/players/{id}", second).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/players/{id}", first).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());

        undo(auth);
        mockMvc.perform(get("/api/v1/players/{id}", first).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void last_describesWhatWillBeUndone() throws Exception {
        String auth = adminAuthHeader();
        Long playerId = createFullPlayer(auth);

        mockMvc.perform(delete("/api/v1/players/{id}", playerId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        MvcResult result = mockMvc.perform(get("/api/v1/undo/last").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        UndoEntryResult last = objectMapper.readValue(result.getResponse().getContentAsString(), UndoEntryResult.class);
        assertThat(last.description()).contains("Brisanje igraca").contains("Canovic");
        assertThat(last.performedBy()).isEqualTo("admin");
    }

    @Test
    void recent_listsOnlyEntriesThatCanStillBeUndone() throws Exception {
        String auth = adminAuthHeader();
        createPlayer(auth, "Prvi", "Prvic");
        createPlayer(auth, "Drugi", "Drugic");

        assertThat(recent(auth)).hasSize(2);

        undo(auth);

        assertThat(recent(auth)).hasSize(1);
    }

    /** Posle ponistavanja se vraca i ono sto je sledece na redu — da dugme ne mora ponovo da pita. */
    @Test
    void undo_responseCarriesNextEntry() throws Exception {
        String auth = adminAuthHeader();
        createPlayer(auth, "Prvi", "Prvic");
        createPlayer(auth, "Drugi", "Drugic");

        mockMvc.perform(post("/api/v1/undo").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Dodavanje igraca Drugi Drugic Drugi"))
                .andExpect(jsonPath("$.next.description").value("Dodavanje igraca Prvi Prvic Prvi"));
    }

    private void undo(String auth) throws Exception {
        mockMvc.perform(post("/api/v1/undo").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
    }

    private UndoEntryResult[] recent(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/undo").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UndoEntryResult[].class);
    }

    private MatchStatus statusOf(String auth, Long matchId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/matches/{id}", matchId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class).status();
    }

    private int playedGamesOf(String auth, Long groupId, Long playerId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/groups/{id}/standings", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        StandingResult[] standings =
                objectMapper.readValue(result.getResponse().getContentAsString(), StandingResult[].class);
        return Arrays.stream(standings)
                .filter(row -> row.playerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Igrac nije u tabeli grupe"))
                .played();
    }

    /** Igrac iz seed grupe koji nema nijedan mec — njega se sme ukloniti iz grupe. */
    private Long playerWithoutMatches(String auth, Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/players?groupId=" + groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        PlayerResult[] players = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerResult[].class);
        return Arrays.stream(players)
                .filter(player -> player.lastName().equals("Jovanović"))
                .findFirst()
                .orElseThrow()
                .id();
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

    private MatchInfo createMatch(String auth, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/matches")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), MatchInfo.class);
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

    /** Igrac sa popunjenim svim poljima — da se vidi da se vraca ceo red, a ne samo ime. */
    private Long createFullPlayer(String auth) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/players")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Aleksandar", "lastName": "Canovic", "nickname": "Cane",
                                 "birthYear": 1994, "phone": "0601234567", "heightCm": 191, "weightKg": 88,
                                 "paymentNote": "6500din", "note": "Ne moze subotom"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class).id();
    }
}
