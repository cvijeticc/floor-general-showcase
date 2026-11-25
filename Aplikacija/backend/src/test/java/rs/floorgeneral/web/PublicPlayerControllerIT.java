package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import rs.floorgeneral.AbstractIntegrationTest;
import rs.floorgeneral.domain.Player;
import rs.floorgeneral.repository.PlayerRepository;
import rs.floorgeneral.repository.StandingRepository;
import rs.floorgeneral.web.dto.site.PublicFormItem;
import rs.floorgeneral.web.dto.site.PublicPlayerProfile;
import rs.floorgeneral.web.dto.site.PublicPlayerSummary;
import rs.floorgeneral.web.dto.site.PublicSeason;

/**
 * Javni spisak igraca i profil (UC-PUB-10, UC-PUB-06): rade bez tokena i objavljuju samo
 * statistiku — nikad visinu, tezinu, godiste, telefon, uplatu ni napomenu (odluka D-21).
 * Seed: db/test/V6 — „Grupa A" (Mare, Pera, Joca), Nidza van grupe, jedan mec Mare–Pera 11:7.
 */
class PublicPlayerControllerIT extends AbstractIntegrationTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private StandingRepository standingRepository;

    private PublicSeason season;

    @BeforeEach
    void recomputeStandings() throws Exception {
        season = currentSeason();
        mockMvc.perform(post("/api/v1/groups/{id}/standings/recompute", season.groups().get(0).id())
                        .header(HttpHeaders.AUTHORIZATION, adminAuthHeader()))
                .andExpect(status().isOk());
    }

    @Test
    void players_withoutToken_listGroupPlayersByPositionWithForm() throws Exception {
        String json = getJson("/api/v1/public/players");
        PublicPlayerSummary[] players = objectMapper.readValue(json, PublicPlayerSummary[].class);

        // Nidza nije ni u jednoj grupi — nema ga na javnom spisku.
        assertThat(players).extracting(p -> p.player().nickname()).containsExactly("Mare", "Pera", "Joca");

        PublicPlayerSummary mare = players[0];
        assertThat(mare.groupName()).isEqualTo("Grupa A");
        assertThat(mare.position()).isEqualTo(1);
        assertThat(mare.wins()).isEqualTo(1);
        assertThat(mare.pointDifference()).isEqualTo(4);
        assertThat(mare.currentStreak()).isEqualTo(1);
        assertThat(mare.form()).singleElement().satisfies(item -> {
            assertThat(item.won()).isTrue();
            assertThat(item.scored()).isEqualTo(11);
            assertThat(item.conceded()).isEqualTo(7);
            assertThat(item.opponent().nickname()).isEqualTo("Pera");
        });
        assertThat(mare.longestWinStreak()).isEqualTo(1);
        assertThat(mare.biggestWin().scored()).isEqualTo(11);
        assertThat(players[1].currentStreak()).isEqualTo(-1);
        assertThat(players[1].form()).extracting(PublicFormItem::won).containsExactly(false);
        assertThat(players[1].biggestWin()).isNull();
        assertThat(players[2].form()).isEmpty();
    }

    /** Spisak radi i pre prvog preracuna tabele — igraci su tu, samo bez mesta i bilansa. */
    @Test
    void players_beforeStandingsRecompute_haveNoPosition() throws Exception {
        standingRepository.deleteAllInBatch();

        PublicPlayerSummary[] players = objectMapper.readValue(getJson("/api/v1/public/players"),
                PublicPlayerSummary[].class);

        assertThat(players).hasSize(3).allSatisfy(p -> {
            assertThat(p.position()).isNull();
            assertThat(p.played()).isZero();
        });
    }

    /** Bez seasonId vazi aktuelna sezona — profil ne mora prvo da pita koja je sezona. */
    @Test
    void profile_withoutSeasonId_usesCurrentSeason() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/public/players/{id}", playerId("Mare")))
                .andExpect(status().isOk())
                .andReturn();

        PublicPlayerProfile profile = objectMapper.readValue(result.getResponse().getContentAsString(),
                PublicPlayerProfile.class);
        assertThat(profile.group().name()).isEqualTo("Grupa A");
    }

    @Test
    void players_neverExposePrivateData() throws Exception {
        assertNoPrivateData(getJson("/api/v1/public/players"));
    }

    @Test
    void profile_withoutToken_containsStandingFormMatchesAndHeadToHead() throws Exception {
        String json = getJson("/api/v1/public/players/" + playerId("Mare"));
        PublicPlayerProfile profile = objectMapper.readValue(json, PublicPlayerProfile.class);

        assertThat(profile.player().nickname()).isEqualTo("Mare");
        assertThat(profile.player().name()).isEqualTo("Marko Marković");
        assertThat(profile.group().name()).isEqualTo("Grupa A");
        assertThat(profile.group().directAdvance()).isEqualTo(2);
        // Cela tabela grupe; red igraca je isti kao na tabu „Tabele".
        assertThat(profile.groupStandings()).extracting(row -> row.player().nickname())
                .containsExactly("Mare", "Pera", "Joca");
        assertThat(profile.groupStandings().get(0).points()).isEqualTo(2);
        assertThat(profile.form()).extracting(PublicFormItem::won).containsExactly(true);
        assertThat(profile.currentStreak()).isEqualTo(1);
        assertThat(profile.longestWinStreak()).isEqualTo(1);
        assertThat(profile.matches()).hasSize(1);
        assertThat(profile.biggestWinMatchId()).isEqualTo(profile.matches().get(0).id());
        assertThat(profile.heaviestLossMatchId()).isNull();
        assertThat(profile.headToHead()).singleElement().satisfies(h2h -> {
            assertThat(h2h.opponent().nickname()).isEqualTo("Pera");
            assertThat(h2h.played()).isEqualTo(1);
            assertThat(h2h.wins()).isEqualTo(1);
            assertThat(h2h.pointsFor()).isEqualTo(11);
            assertThat(h2h.pointsAgainst()).isEqualTo(7);
        });
        assertNoPrivateData(json);
    }

    /** Joca je u grupi, ali jos nije igrao — profil postoji, prazan, i bez njegove napomene. */
    @Test
    void profile_playerWithoutMatches_isEmptyButVisible() throws Exception {
        String json = getJson("/api/v1/public/players/" + playerId("Joca"));
        PublicPlayerProfile profile = objectMapper.readValue(json, PublicPlayerProfile.class);

        assertThat(profile.group().name()).isEqualTo("Grupa A");
        assertThat(profile.matches()).isEmpty();
        assertThat(profile.form()).isEmpty();
        assertThat(profile.currentStreak()).isZero();
        assertThat(profile.biggestWinMatchId()).isNull();
        assertNoPrivateData(json);
    }

    @Test
    void profile_playerOutsideSeason_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/players/{id}", playerId("Nidza")).param("seasonId", seasonId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/public/players/{id}", 999_999L).param("seasonId", seasonId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownSeason_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/public/players").param("seasonId", "999999"))
                .andExpect(status().isNotFound());
    }

    private void assertNoPrivateData(String json) {
        assertThat(json)
                .doesNotContain("heightCm").doesNotContain("weightKg").doesNotContain("birthYear")
                .doesNotContain("phone").doesNotContain("+381601111111")
                .doesNotContain("6500din").doesNotContain("Ne moze subotom");
    }

    private String getJson(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path).param("seasonId", seasonId()))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private String seasonId() {
        return season.id().toString();
    }

    private Long playerId(String nickname) {
        return playerRepository.findAll().stream()
                .filter(p -> p.getNickname().equals(nickname))
                .map(Player::getPlayerID)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Seed igrac '" + nickname + "' nije pronadjen"));
    }

    private PublicSeason currentSeason() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/public/season"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), PublicSeason.class);
    }
}
