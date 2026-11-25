package rs.floorgeneral.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import rs.floorgeneral.domain.Group;
import rs.floorgeneral.repository.GroupRepository;

/**
 * Pri svakom pokretanju preracunava tabelu svake grupe. Rezultati koje unese Flyway seed
 * ({@code db/dev/V12–V14}, {@code db/prod/V14–V15}) ne prolaze kroz servis, pa bi tabela ostala na
 * stanju od pre migracije. Tako je dev baza posle {@code db/dev/V14} pokazivala 2 odigrana meca
 * umesto 4: tabela je bila izracunata posle V12, a ranija verzija je preskakala grupe koje vec
 * imaju tabelu (prod je to zaobisao sa {@code delete from standings} na kraju V15).
 * <p>
 * Tabela je izvedena iz meceva (FR-6.5.1), a preracun je idempotentan (BR-12.3), pa ponovno
 * racunanje nikad ne menja ono sto je vec tacno. Testovi imaju sopstvene seed-ove i ocekivanja.
 */
@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
@Slf4j
public class StandingsBootstrap implements ApplicationRunner {

    private final GroupRepository groupRepository;
    private final StandingsService standingsService;

    @Override
    public void run(ApplicationArguments args) {
        int count = 0;
        for (Group group : groupRepository.findAll()) {
            standingsService.recompute(group.getGroupID());
            count++;
        }
        log.info("Preracunate tabele grupa pri pokretanju: {}.", count);
    }
}
