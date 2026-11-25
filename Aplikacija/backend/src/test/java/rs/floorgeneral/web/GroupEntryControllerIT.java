package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;

/**
 * UC-ADM-07. Seed: faza "Grupna faza" ima grupu "Grupa A" sa 3 igraca, od kojih su Marković
 * i Petrović odigrali mec — na njima se proverava da se igrac sa mecom ne uklanja iz grupe.
 */
class GroupEntryControllerIT extends AbstractIntegrationTest {

    @Test
    void unassignedPlayers_listsOnlyActivePlayersWithoutGroupInThatStage() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = seededStageId(auth);

        // Pre dodavanja: sva tri aktivna igraca su u grupi, a cetvrti je ODUSTAO -> lista je prazna.
        assertThat(unassigned(auth, stageId)).isEmpty();

        createPlayer(auth, "Stefan", "Stefanović");

        PlayerResult[] afterNewPlayer = unassigned(auth, stageId);
        assertThat(afterNewPlayer).hasSize(1);
        assertThat(afterNewPlayer[0].fullName()).isEqualTo("Stefan Stefanović");
    }

    @Test
    void unassignedPlayers_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stages/{id}/unassigned-players", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void unassignedPlayers_unknownStage_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/stages/{id}/unassigned-players", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignPlayers_addsAllPlayersAtOnce() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona za rasporedjivanje 1", 10);
        Long groupId = createGroup(auth, stageId, "Grupa A");
        Long first = createPlayer(auth, "Stefan", "Stefanović");
        Long second = createPlayer(auth, "Luka", "Lukić");

        GroupInfo group = assign(auth, groupId, first, second);

        assertThat(group.playerCount()).isEqualTo(2L);
        // Seed igraci nisu u grupama OVE faze, pa i dalje stoje na listi — bitno je da novih dvoje nema.
        assertThat(Arrays.stream(unassigned(auth, stageId)).map(PlayerResult::lastName))
                .doesNotContain("Stefanović", "Lukić");

        PlayerResult[] inGroup = playersOfGroup(auth, groupId);
        assertThat(Arrays.stream(inGroup).map(PlayerResult::lastName)).containsExactly("Lukić", "Stefanović");
    }

    @Test
    void assignPlayers_withdrawnPlayer_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona za rasporedjivanje 2", 10);
        Long groupId = createGroup(auth, stageId, "Grupa A");
        Long playerId = createPlayer(auth, "Stefan", "Stefanović");

        mockMvc.perform(patch("/api/v1/players/{id}/status", playerId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"WITHDRAWN\", \"note\": \"Odustao\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/groups/{id}/players", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\": [%d]}".formatted(playerId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void assignPlayers_sameStageTwoGroups_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona za rasporedjivanje 3", 10);
        Long firstGroup = createGroup(auth, stageId, "Grupa A");
        Long secondGroup = createGroup(auth, stageId, "Grupa B");
        Long playerId = createPlayer(auth, "Stefan", "Stefanović");

        assign(auth, firstGroup, playerId);

        mockMvc.perform(post("/api/v1/groups/{id}/players", secondGroup)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\": [%d]}".formatted(playerId)))
                .andExpect(status().isConflict());
    }

    /** Isti igrac u dve razlicite faze je dozvoljen — bez toga sezona sa dve grupne faze ne radi (D-04). */
    @Test
    void assignPlayers_samePlayerInTwoStages_isAllowed() throws Exception {
        String auth = adminAuthHeader();
        Long firstStage = createGroupStage(auth, "Sezona za rasporedjivanje 4", 10);
        Long secondStage = createGroupStage(auth, "Sezona za rasporedjivanje 5", 10);
        Long firstGroup = createGroup(auth, firstStage, "Grupa A");
        Long secondGroup = createGroup(auth, secondStage, "Grupa A");
        Long playerId = createPlayer(auth, "Stefan", "Stefanović");

        assign(auth, firstGroup, playerId);
        GroupInfo second = assign(auth, secondGroup, playerId);

        assertThat(second.playerCount()).isEqualTo(1L);
    }

    @Test
    void assignPlayers_overGroupSize_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = createSeason(auth, "Sezona sa malim grupama");
        Long stageId = createStage(auth, seasonId, """
                {"name": "Grupna faza", "type": "GROUP", "orderNo": 1, "groupCount": 4,
                 "groupSize": 2, "roundCount": 1, "directAdvance": 2, "additionalAdvance": 0}
                """).id();
        Long groupId = createGroup(auth, stageId, "Grupa A");
        Long first = createPlayer(auth, "Stefan", "Stefanović");
        Long second = createPlayer(auth, "Luka", "Lukić");
        Long third = createPlayer(auth, "Ivan", "Ivanović");

        mockMvc.perform(post("/api/v1/groups/{id}/players", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\": [%d, %d, %d]}".formatted(first, second, third)))
                .andExpect(status().isConflict());

        assertThat(playersOfGroup(auth, groupId)).isEmpty();
    }

    @Test
    void assignPlayers_withEmptyList_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona za rasporedjivanje 6", 10);
        Long groupId = createGroup(auth, stageId, "Grupa A");

        mockMvc.perform(post("/api/v1/groups/{id}/players", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.playerIds").isNotEmpty());
    }

    @Test
    void assignPlayers_unknownGroup_returns404() throws Exception {
        String auth = adminAuthHeader();
        Long playerId = createPlayer(auth, "Stefan", "Stefanović");

        mockMvc.perform(post("/api/v1/groups/{id}/players", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\": [%d]}".formatted(playerId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void removePlayer_withoutMatches_freesTheSpot() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona za uklanjanje", 10);
        Long groupId = createGroup(auth, stageId, "Grupa A");
        Long playerId = createPlayer(auth, "Stefan", "Stefanović");
        assign(auth, groupId, playerId);

        MvcResult result = mockMvc.perform(delete("/api/v1/groups/{groupId}/players/{playerId}", groupId, playerId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        GroupInfo group = objectMapper.readValue(result.getResponse().getContentAsString(), GroupInfo.class);
        assertThat(group.playerCount()).isZero();
        assertThat(Arrays.stream(unassigned(auth, stageId)).map(PlayerResult::lastName))
                .contains("Stefanović");
    }

    @Test
    void removePlayer_withMatchInStage_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = seededStageId(auth);
        Long groupId = groupIdOfStage(auth, stageId);
        Long playerWithMatch = Arrays.stream(playersOfGroup(auth, groupId))
                .filter(p -> p.lastName().equals("Marković"))
                .findFirst()
                .orElseThrow()
                .id();

        mockMvc.perform(delete("/api/v1/groups/{groupId}/players/{playerId}", groupId, playerWithMatch)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void removePlayer_notInGroup_returns404() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona za uklanjanje 2", 10);
        Long groupId = createGroup(auth, stageId, "Grupa A");
        Long playerId = createPlayer(auth, "Stefan", "Stefanović");

        mockMvc.perform(delete("/api/v1/groups/{groupId}/players/{playerId}", groupId, playerId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    private PlayerResult[] unassigned(String auth, Long stageId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stages/{id}/unassigned-players", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PlayerResult[].class);
    }

    private PlayerResult[] playersOfGroup(String auth, Long groupId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/players?groupId=" + groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PlayerResult[].class);
    }

    private GroupInfo assign(String auth, Long groupId, Long... playerIds) throws Exception {
        String ids = Arrays.stream(playerIds).map(String::valueOf).reduce((a, b) -> a + ", " + b).orElseThrow();
        MvcResult result = mockMvc.perform(post("/api/v1/groups/{id}/players", groupId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\": [%s]}".formatted(ids)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), GroupInfo.class);
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

    private Long groupIdOfStage(String auth, Long stageId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stages/{id}/groups", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asLong();
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
