package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import rs.floorgeneral.web.dto.league.LeagueInfo;
import rs.floorgeneral.web.dto.league.LeagueResult;

/**
 * Seed (src/test/resources/db/test/V2__seed_test_data.sql) sadrzi tacno 2 lige:
 * "Floor General 1v1 Liga" i "Floor General 1v1 Liga - Rekreativna divizija".
 */
class LeagueControllerIT extends AbstractIntegrationTest {

    private static final String SEEDED_LEAGUE_NAME = "Floor General 1v1 Liga";

    @Test
    void search_returnsAllSeededLeagues() throws Exception {
        String auth = adminAuthHeader();

        MvcResult result = mockMvc.perform(get("/api/v1/leagues").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        LeagueResult[] leagues = objectMapper.readValue(result.getResponse().getContentAsString(), LeagueResult[].class);
        assertThat(leagues).hasSize(2);

        LeagueResult seeded = Arrays.stream(leagues)
                .filter(l -> l.name().equals(SEEDED_LEAGUE_NAME))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed liga '" + SEEDED_LEAGUE_NAME + "' nije pronadjena u rezultatu"));

        assertThat(seeded.id()).isNotNull().isPositive();
        assertThat(seeded.name()).isEqualTo(SEEDED_LEAGUE_NAME);
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/leagues"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void findById_returnsMatchingLeagueInfo() throws Exception {
        String auth = adminAuthHeader();
        LeagueResult seeded = findSeededLeague(auth);

        MvcResult result = mockMvc.perform(get("/api/v1/leagues/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        LeagueInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), LeagueInfo.class);
        assertThat(info.id()).isEqualTo(seeded.id());
        assertThat(info.name()).isEqualTo(SEEDED_LEAGUE_NAME);
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/leagues/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_persistsNewLeague() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"name": "Testna Liga za Veterane"}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/leagues")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        LeagueInfo created = objectMapper.readValue(createResult.getResponse().getContentAsString(), LeagueInfo.class);
        assertThat(created.id()).isNotNull().isPositive();
        assertThat(created.name()).isEqualTo("Testna Liga za Veterane");

        // Simulacija: potvrdi da je zaista upisano u bazu, ne samo vraceno u odgovoru.
        MvcResult listResult = mockMvc.perform(get("/api/v1/leagues").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        LeagueResult[] leagues = objectMapper.readValue(listResult.getResponse().getContentAsString(), LeagueResult[].class);
        assertThat(leagues).hasSize(3);
        assertThat(Arrays.stream(leagues).anyMatch(l -> l.id().equals(created.id()) && l.name().equals("Testna Liga za Veterane")))
                .isTrue();
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"name": ""}
                """;

        mockMvc.perform(post("/api/v1/leagues")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void update_changesLeagueNameAndPersists() throws Exception {
        String auth = adminAuthHeader();
        Long newLeagueId = createLeague(auth, "Liga za Izmenu");

        String updateBody = """
                {"name": "Liga posle izmene"}
                """;
        MvcResult updateResult = mockMvc.perform(put("/api/v1/leagues/{id}", newLeagueId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn();

        LeagueInfo updated = objectMapper.readValue(updateResult.getResponse().getContentAsString(), LeagueInfo.class);
        assertThat(updated.id()).isEqualTo(newLeagueId);
        assertThat(updated.name()).isEqualTo("Liga posle izmene");

        MvcResult getResult = mockMvc.perform(get("/api/v1/leagues/{id}", newLeagueId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        LeagueInfo persisted = objectMapper.readValue(getResult.getResponse().getContentAsString(), LeagueInfo.class);
        assertThat(persisted.name()).isEqualTo("Liga posle izmene");
    }

    @Test
    void delete_removesLeague() throws Exception {
        String auth = adminAuthHeader();
        Long newLeagueId = createLeague(auth, "Liga za Brisanje");

        mockMvc.perform(delete("/api/v1/leagues/{id}", newLeagueId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/leagues/{id}", newLeagueId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    private LeagueResult findSeededLeague(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/leagues").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        LeagueResult[] leagues = objectMapper.readValue(result.getResponse().getContentAsString(), LeagueResult[].class);
        return Arrays.stream(leagues)
                .filter(l -> l.name().equals(SEEDED_LEAGUE_NAME))
                .findFirst()
                .orElseThrow();
    }

    private Long createLeague(String auth, String name) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new CreateLeaguePayload(name));
        MvcResult result = mockMvc.perform(post("/api/v1/leagues")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), LeagueInfo.class).id();
    }

    private record CreateLeaguePayload(String name) {
    }
}
