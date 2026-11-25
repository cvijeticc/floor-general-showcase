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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.domain.PlayerStatus;
import rs.floorgeneral.web.dto.player.PlayerInfo;
import rs.floorgeneral.web.dto.player.PlayerResult;

/**
 * Seed (src/test/resources/db/test/V6__seed_test_players.sql) sadrzi tacno 4 igraca:
 * Marković i Petrović su odigrali mec i u grupi su, Jovanović je samo u grupi,
 * Nikolić nije ni u grupi ni u mecu i ima status WITHDRAWN.
 */
class PlayerControllerIT extends AbstractIntegrationTest {

    @Test
    void search_returnsAllSeededPlayersSortedByLastName() throws Exception {
        String auth = adminAuthHeader();

        PlayerResult[] players = searchPlayers(auth, "");

        assertThat(players).hasSize(4);
        assertThat(Arrays.stream(players).map(PlayerResult::lastName))
                .containsExactly("Jovanović", "Marković", "Nikolić", "Petrović");
        assertThat(players[1].fullName()).isEqualTo("Marko Marković");
        assertThat(players[1].displayName()).isEqualTo("Marko Marković Mare");
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void search_byPartOfName_isCaseInsensitive() throws Exception {
        String auth = adminAuthHeader();

        PlayerResult[] players = searchPlayers(auth, "?search=MARK");

        assertThat(players).hasSize(1);
        assertThat(players[0].fullName()).isEqualTo("Marko Marković");
    }

    @Test
    void search_byStatus_returnsOnlyMatchingPlayers() throws Exception {
        String auth = adminAuthHeader();

        PlayerResult[] withdrawn = searchPlayers(auth, "?status=WITHDRAWN");

        assertThat(withdrawn).hasSize(1);
        assertThat(withdrawn[0].lastName()).isEqualTo("Nikolić");
        assertThat(withdrawn[0].status()).isEqualTo(PlayerStatus.WITHDRAWN);
    }

    @Test
    void search_byGroup_returnsOnlyPlayersEnteredInThatGroup() throws Exception {
        String auth = adminAuthHeader();
        Long groupId = seededGroupId(auth);

        PlayerResult[] inGroup = searchPlayers(auth, "?groupId=" + groupId);

        assertThat(Arrays.stream(inGroup).map(PlayerResult::lastName))
                .containsExactly("Jovanović", "Marković", "Petrović");

        PlayerResult[] inUnknownGroup = searchPlayers(auth, "?groupId=999999");
        assertThat(inUnknownGroup).isEmpty();
    }

    @Test
    void findById_returnsFullPlayerInfo() throws Exception {
        String auth = adminAuthHeader();
        PlayerResult seeded = findSeeded(auth, "Marković");

        MvcResult result = mockMvc.perform(get("/api/v1/players/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        PlayerInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class);
        assertThat(info.firstName()).isEqualTo("Marko");
        assertThat(info.nickname()).isEqualTo("Mare");
        assertThat(info.phone()).isEqualTo("+381601111111");
        assertThat(info.birthYear()).isEqualTo(1995);
        assertThat(info.heightCm()).isEqualTo(190);
        assertThat(info.weightKg()).isEqualTo(85);
        assertThat(info.paymentNote()).isEqualTo("6500din");
        assertThat(info.status()).isEqualTo(PlayerStatus.ACTIVE);
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/players/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_persistsNewPlayerAsActive() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"firstName": "Stefan", "lastName": "Stefanović", "nickname": "Stefke",
                 "birthYear": 1999, "phone": "+381603333333", "heightCm": 192, "weightKg": 88,
                 "paymentNote": "6500din", "note": "Ne moze subotom pre 15h"}
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/players")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        PlayerInfo created = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class);
        assertThat(created.id()).isNotNull().isPositive();
        assertThat(created.fullName()).isEqualTo("Stefan Stefanović");
        assertThat(created.displayName()).isEqualTo("Stefan Stefanović Stefke");
        assertThat(created.birthYear()).isEqualTo(1999);
        assertThat(created.note()).isEqualTo("Ne moze subotom pre 15h");
        assertThat(created.status()).isEqualTo(PlayerStatus.ACTIVE);

        assertThat(searchPlayers(auth, "")).hasSize(5);
    }

    @Test
    void create_withBlankLastName_returns400() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"firstName": "Stefan", "lastName": "", "nickname": "Stefke"}
                """;

        mockMvc.perform(post("/api/v1/players")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.lastName").isNotEmpty());
    }

    @Test
    void create_withoutNickname_returns400() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"firstName": "Stefan", "lastName": "Stefanović"}
                """;

        mockMvc.perform(post("/api/v1/players")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nickname").isNotEmpty());
    }

    /** V5 / D-16 — godiste je broj, ne datum; granice su 1900..2020. */
    @Test
    void create_withImpossibleBirthYear_returns400() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"firstName": "Stefan", "lastName": "Stefanović", "nickname": "Stefke", "birthYear": 2999}
                """;

        mockMvc.perform(post("/api/v1/players")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.birthYear").isNotEmpty());
    }

    /** Admin cesce zna nadimak nego prezime — pretraga mora da ga pokriva (UC-FE-05). */
    @Test
    void search_byNickname_findsPlayer() throws Exception {
        String auth = adminAuthHeader();

        PlayerResult[] players = searchPlayers(auth, "?search=joca");

        assertThat(players).hasSize(1);
        assertThat(players[0].lastName()).isEqualTo("Jovanović");
    }

    @Test
    void update_changesDataButKeepsStatus() throws Exception {
        String auth = adminAuthHeader();
        PlayerResult seeded = findSeeded(auth, "Nikolić");
        String updateBody = """
                {"firstName": "Nikola", "lastName": "Nikolić", "nickname": "Nidza",
                 "birthYear": 1992, "phone": "+381604444444"}
                """;

        MvcResult result = mockMvc.perform(put("/api/v1/players/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn();

        PlayerInfo updated = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class);
        assertThat(updated.phone()).isEqualTo("+381604444444");
        assertThat(updated.status()).isEqualTo(PlayerStatus.WITHDRAWN);
    }

    @Test
    void changeStatus_persistsNewStatus() throws Exception {
        String auth = adminAuthHeader();
        PlayerResult seeded = findSeeded(auth, "Jovanović");
        String body = """
                {"status": "DISQUALIFIED", "note": "Neprimereno ponasanje na terenu"}
                """;

        mockMvc.perform(patch("/api/v1/players/{id}/status", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISQUALIFIED"));

        MvcResult result = mockMvc.perform(get("/api/v1/players/{id}", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        PlayerInfo persisted = objectMapper.readValue(result.getResponse().getContentAsString(), PlayerInfo.class);
        assertThat(persisted.status()).isEqualTo(PlayerStatus.DISQUALIFIED);
    }

    @Test
    void changeStatus_withoutStatus_returns400() throws Exception {
        String auth = adminAuthHeader();
        PlayerResult seeded = findSeeded(auth, "Jovanović");

        mockMvc.perform(patch("/api/v1/players/{id}/status", seeded.id())
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"bez statusa\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").isNotEmpty());
    }

    @Test
    void delete_playerWithoutMatchesAndGroups_removesPlayer() throws Exception {
        String auth = adminAuthHeader();
        PlayerResult seeded = findSeeded(auth, "Nikolić");

        mockMvc.perform(delete("/api/v1/players/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/players/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_playerWithPlayedMatch_returns409() throws Exception {
        String auth = adminAuthHeader();
        PlayerResult seeded = findSeeded(auth, "Marković");

        mockMvc.perform(delete("/api/v1/players/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void delete_playerAssignedToGroup_returns409() throws Exception {
        String auth = adminAuthHeader();
        PlayerResult seeded = findSeeded(auth, "Jovanović");

        mockMvc.perform(delete("/api/v1/players/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isConflict());
    }

    private PlayerResult[] searchPlayers(String auth, String queryString) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/players" + queryString)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PlayerResult[].class);
    }

    private PlayerResult findSeeded(String auth, String lastName) throws Exception {
        return Arrays.stream(searchPlayers(auth, ""))
                .filter(p -> p.lastName().equals(lastName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed igrac '" + lastName + "' nije pronadjen"));
    }

    /** Grupa iz test seeda se pronalazi preko igraca koji su u njoj — grupe jos nemaju svoj API. */
    private Long seededGroupId(String auth) throws Exception {
        for (long candidate = 1L; candidate <= 20L; candidate++) {
            PlayerResult[] players = searchPlayers(auth, "?groupId=" + candidate);
            if (players.length == 3) {
                return candidate;
            }
        }
        throw new AssertionError("Test grupa sa 3 igraca nije pronadjena");
    }
}
