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
import rs.floorgeneral.web.dto.court.CourtInfo;
import rs.floorgeneral.web.dto.court.CourtResult;

/**
 * Seed (src/test/resources/db/test/V2__seed_test_data.sql) sadrzi tacno 3 terena, medju njima
 * "Hala Pinki" sa poznatom adresom — koristi se kao konkretan clan niza u testovima pretrage.
 */
class CourtControllerIT extends AbstractIntegrationTest {

    private static final String SEEDED_COURT_NAME = "Hala Pinki";
    private static final String SEEDED_COURT_ADDRESS = "Trg Nikole Pašića 1, Zemun";

    @Test
    void search_returnsAllSeededCourts() throws Exception {
        String auth = adminAuthHeader();

        MvcResult result = mockMvc.perform(get("/api/v1/courts").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        CourtResult[] courts = objectMapper.readValue(result.getResponse().getContentAsString(), CourtResult[].class);
        assertThat(courts).hasSize(3);

        CourtResult seeded = Arrays.stream(courts)
                .filter(c -> c.name().equals(SEEDED_COURT_NAME))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed teren '" + SEEDED_COURT_NAME + "' nije pronadjen u rezultatu"));

        assertThat(seeded.id()).isNotNull().isPositive();
        assertThat(seeded.name()).isEqualTo(SEEDED_COURT_NAME);
        assertThat(seeded.address()).isEqualTo(SEEDED_COURT_ADDRESS);
    }

    @Test
    void search_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/courts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void findById_returnsMatchingCourtInfo() throws Exception {
        String auth = adminAuthHeader();
        CourtResult seeded = findSeededCourt(auth);

        MvcResult result = mockMvc.perform(get("/api/v1/courts/{id}", seeded.id()).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();

        CourtInfo info = objectMapper.readValue(result.getResponse().getContentAsString(), CourtInfo.class);
        assertThat(info.id()).isEqualTo(seeded.id());
        assertThat(info.name()).isEqualTo(SEEDED_COURT_NAME);
        assertThat(info.address()).isEqualTo(SEEDED_COURT_ADDRESS);
    }

    @Test
    void findById_unknownId_returns404() throws Exception {
        String auth = adminAuthHeader();

        mockMvc.perform(get("/api/v1/courts/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void create_persistsNewCourt() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"name": "Teren Kalemegdan", "address": "Kalemegdan bb, Beograd"}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/courts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();

        CourtInfo created = objectMapper.readValue(createResult.getResponse().getContentAsString(), CourtInfo.class);
        assertThat(created.id()).isNotNull().isPositive();
        assertThat(created.name()).isEqualTo("Teren Kalemegdan");
        assertThat(created.address()).isEqualTo("Kalemegdan bb, Beograd");

        MvcResult listResult = mockMvc.perform(get("/api/v1/courts").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        CourtResult[] courts = objectMapper.readValue(listResult.getResponse().getContentAsString(), CourtResult[].class);
        assertThat(courts).hasSize(4);
        assertThat(Arrays.stream(courts).anyMatch(c -> c.id().equals(created.id()) && c.name().equals("Teren Kalemegdan")))
                .isTrue();
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        String auth = adminAuthHeader();
        String requestBody = """
                {"name": "", "address": "Neka adresa"}
                """;

        mockMvc.perform(post("/api/v1/courts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").isNotEmpty());
    }

    @Test
    void update_changesCourtAddressAndPersists() throws Exception {
        String auth = adminAuthHeader();
        Long newCourtId = createCourt(auth, "Teren za Izmenu", "Stara adresa 1");

        String updateBody = """
                {"name": "Teren za Izmenu", "address": "Nova adresa 2"}
                """;
        MvcResult updateResult = mockMvc.perform(put("/api/v1/courts/{id}", newCourtId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andReturn();

        CourtInfo updated = objectMapper.readValue(updateResult.getResponse().getContentAsString(), CourtInfo.class);
        assertThat(updated.address()).isEqualTo("Nova adresa 2");

        MvcResult getResult = mockMvc.perform(get("/api/v1/courts/{id}", newCourtId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        CourtInfo persisted = objectMapper.readValue(getResult.getResponse().getContentAsString(), CourtInfo.class);
        assertThat(persisted.address()).isEqualTo("Nova adresa 2");
    }

    @Test
    void delete_removesCourt() throws Exception {
        String auth = adminAuthHeader();
        Long newCourtId = createCourt(auth, "Teren za Brisanje", "Adresa bb");

        mockMvc.perform(delete("/api/v1/courts/{id}", newCourtId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/courts/{id}", newCourtId).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    private CourtResult findSeededCourt(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/courts").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        CourtResult[] courts = objectMapper.readValue(result.getResponse().getContentAsString(), CourtResult[].class);
        return Arrays.stream(courts)
                .filter(c -> c.name().equals(SEEDED_COURT_NAME))
                .findFirst()
                .orElseThrow();
    }

    private Long createCourt(String auth, String name, String address) throws Exception {
        String requestBody = objectMapper.writeValueAsString(new CreateCourtPayload(name, address));
        MvcResult result = mockMvc.perform(post("/api/v1/courts")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), CourtInfo.class).id();
    }

    private record CreateCourtPayload(String name, String address) {
    }
}
