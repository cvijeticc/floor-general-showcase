package rs.floorgeneral.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Male ciste funkcije javnog dela: redosled grupa i deljenje imena iz prijave. */
class PublicHelpersTest {

    @Test
    void compareNatural_putsGroup2BeforeGroup10() {
        List<String> sorted = Stream.of("Grupa 10", "Grupa 2", "Grupa 1", "Grupa 9")
                .sorted(PublicSiteService::compareNatural)
                .toList();

        assertThat(sorted).containsExactly("Grupa 1", "Grupa 2", "Grupa 9", "Grupa 10");
    }

    @Test
    void compareNatural_fallsBackToAlphabeticalWithoutNumbers() {
        assertThat(PublicSiteService.compareNatural("Grupa A", "Grupa B")).isNegative();
    }

    @Test
    void splitName_lastWordIsLastName() {
        assertThat(ApplicationService.splitName("  Ana   Marija Petrović ")).containsExactly("Ana Marija", "Petrović");
    }

    @Test
    void splitName_singleWordKeepsPlaceholderLastName() {
        assertThat(ApplicationService.splitName("Arči")).containsExactly("Arči", "-");
    }
}
