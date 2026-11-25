package rs.floorgeneral;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import rs.floorgeneral.web.dto.league.LeagueResult;
import rs.floorgeneral.web.dto.season.SeasonInfo;
import rs.floorgeneral.web.dto.season.SeasonResult;
import rs.floorgeneral.web.dto.stage.StageInfo;
import rs.floorgeneral.web.dto.stage.StageResult;

/**
 * Bazna klasa za integracione testove: podize pravi Spring kontekst + JWT security lanac + pravu
 * Postgres bazu (Testcontainers, baseline sema preko Flyway-a + deterministicki seed iz
 * src/test/resources/db/test). Svaki test metod radi u sopstvenoj transakciji koja se rolluje
 * nazad na kraju, tako da seed podaci ostaju netaknuti izmedju testova i test klasa.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
@Tag("integration")
public abstract class AbstractIntegrationTest {

    /** Sezona iz test seeda (db/test/V4__seed_test_players.sql). */
    protected static final String SEEDED_SEASON = "Test sezona";

    protected static final String MAIN_LEAGUE = "Floor General 1v1 Liga";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** Prijavljuje se podrazumevanim admin nalogom (kreira ga AdminSeeder) i vraca "Bearer &lt;token&gt;". */
    protected String adminAuthHeader() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new LoginPayload("admin", "admin123"));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return "Bearer " + json.get("accessToken").asText();
    }

    protected Long leagueId(String auth, String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/leagues").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        LeagueResult[] leagues = objectMapper.readValue(result.getResponse().getContentAsString(), LeagueResult[].class);
        return Arrays.stream(leagues)
                .filter(l -> l.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed liga '" + name + "' nije pronadjena"))
                .id();
    }

    protected Long seededSeasonId(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/seasons").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        SeasonResult[] seasons = objectMapper.readValue(result.getResponse().getContentAsString(), SeasonResult[].class);
        return Arrays.stream(seasons)
                .filter(s -> s.name().equals(SEEDED_SEASON))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed sezona nije pronadjena"))
                .id();
    }

    protected Long seededStageId(String auth) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/seasons/{id}/stages", seededSeasonId(auth))
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andReturn();
        StageResult[] stages = objectMapper.readValue(result.getResponse().getContentAsString(), StageResult[].class);
        return stages[0].id();
    }

    /** Nova sezona u glavnoj ligi, u statusu DRAFT — da testovi ne diraju seed podatke. */
    protected Long createSeason(String auth, String name) throws Exception {
        String body = """
                {"leagueId": %d, "name": "%s", "playerCount": 100}
                """.formatted(leagueId(auth, MAIN_LEAGUE), name);
        MvcResult result = mockMvc.perform(post("/api/v1/seasons")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), SeasonInfo.class).id();
    }

    protected StageInfo createStage(String auth, Long seasonId, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/seasons/{id}/stages", seasonId)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), StageInfo.class);
    }

    /** Prakticna precica: nova sezona + grupna faza sa zadatim brojem grupa (velicina grupe 10). */
    protected Long createGroupStage(String auth, String seasonName, int groupCount) throws Exception {
        Long seasonId = createSeason(auth, seasonName);
        return createStage(auth, seasonId, """
                {"name": "Grupna faza", "type": "GROUP", "orderNo": 1, "groupCount": %d,
                 "groupSize": 10, "roundCount": 2, "directAdvance": 6, "additionalAdvance": 4}
                """.formatted(groupCount)).id();
    }

    private record LoginPayload(String username, String password) {
    }
}
