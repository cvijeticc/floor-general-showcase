package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.web.dto.group.GroupInfo;
import rs.floorgeneral.web.dto.group.GroupResult;

/**
 * Seed: faza "Grupna faza" ima groupCount = 1 i vec napravljenu "Grupa A" sa 3 igraca
 * i jednim odigranim mecem. Zato se na njoj proveravaju i popunjen format i odbijeno brisanje.
 */
class GroupControllerIT extends AbstractIntegrationTest {

    @Test
    void searchByStage_returnsSeededGroupWithPlayerCount() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = seededStageId(auth);

        GroupResult[] groups = groupsOfStage(auth, stageId);

        assertThat(groups).hasSize(1);
        assertThat(groups[0].name()).isEqualTo("Grupa A");
        assertThat(groups[0].playerCount()).isEqualTo(3L);
        assertThat(groups[0].groupSize()).isEqualTo(4);
    }

    @Test
    void searchByStage_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stages/{id}/groups", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void searchBySeason_returnsGroupsAcrossStages() throws Exception {
        String auth = adminAuthHeader();
        Long seasonId = seededSeasonId(auth);

        MvcResult result = mockMvc.perform(get("/api/v1/seasons/{id}/groups", seasonId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        GroupResult[] groups = objectMapper.readValue(result.getResponse().getContentAsString(), GroupResult[].class);
        assertThat(groups).hasSize(1);
        assertThat(groups[0].stageName()).isEqualTo("Grupna faza");
    }

    @Test
    void findById_returnsSeasonAndStageContext() throws Exception {
        String auth = adminAuthHeader();
        GroupResult seeded = groupsOfStage(auth, seededStageId(auth))[0];

        MvcResult result = mockMvc.perform(get("/api/v1/groups/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        GroupInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), GroupInfo.class);
        assertThat(info.seasonName()).isEqualTo("Test sezona");
        assertThat(info.stageName()).isEqualTo("Grupna faza");
        assertThat(info.playerCount()).isEqualTo(3L);
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/groups/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_persistsGroupInStage() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona sa grupama 1", 10);

        GroupInfo created = createGroup(auth, stageId, "Grupa A");

        assertThat(created.id()).isNotNull().isPositive();
        assertThat(created.stageId()).isEqualTo(stageId);
        assertThat(created.playerCount()).isZero();
        assertThat(groupsOfStage(auth, stageId)).hasSize(1);
    }

    @Test
    void create_duplicateNameInSameStage_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona sa grupama 2", 10);
        createGroup(auth, stageId, "Grupa A");

        mockMvc.perform(post("/api/v1/stages/{id}/groups", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"grupa a\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void create_sameNameInAnotherStage_isAllowed() throws Exception {
        String auth = adminAuthHeader();
        Long firstStage = createGroupStage(auth, "Sezona sa grupama 3", 10);
        Long secondStage = createGroupStage(auth, "Sezona sa grupama 4", 10);

        createGroup(auth, firstStage, "Grupa A");
        GroupInfo second = createGroup(auth, secondStage, "Grupa A");

        assertThat(second.stageId()).isEqualTo(secondStage);
    }

    @Test
    void create_moreGroupsThanFormatAllows_returns409() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = seededStageId(auth);

        mockMvc.perform(post("/api/v1/stages/{id}/groups", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Grupa B\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona sa grupama 5", 10);

        mockMvc.perform(post("/api/v1/stages/{id}/groups", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void update_renamesGroup() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona sa grupama 6", 10);
        GroupInfo group = createGroup(auth, stageId, "Grupa A");

        MvcResult result = mockMvc.perform(put("/api/v1/groups/{id}", group.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Grupa A1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        GroupInfo updated = objectMapper.readValue(result.getResponse().getContentAsString(), GroupInfo.class);
        assertThat(updated.name()).isEqualTo("Grupa A1");
    }

    @Test
    void delete_groupWithPlayers_returns409() throws Exception {
        String auth = adminAuthHeader();
        GroupResult seeded = groupsOfStage(auth, seededStageId(auth))[0];

        mockMvc.perform(delete("/api/v1/groups/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_emptyGroup_removesGroup() throws Exception {
        String auth = adminAuthHeader();
        Long stageId = createGroupStage(auth, "Sezona sa grupama 7", 10);
        GroupInfo group = createGroup(auth, stageId, "Grupa Z");

        mockMvc.perform(delete("/api/v1/groups/{id}", group.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/groups/{id}", group.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    private GroupResult[] groupsOfStage(String auth, Long stageId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/stages/{id}/groups", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), GroupResult[].class);
    }

    private GroupInfo createGroup(String auth, Long stageId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/stages/{id}/groups", stageId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), GroupInfo.class);
    }
}
