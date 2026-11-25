package rs.floorgeneral;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Efemerna test baza — Testcontainers digne pravi Postgres 16 u Docker-u samo za vreme testova.
 * Nikad se ne dodiruje dev baza iz docker-compose.yml. Spring Boot cache-ira aplikacioni kontekst
 * po konfiguraciji, pa se isti kontejner deli izmedju svih integracionih test klasa u istom runu.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16"));
    }
}
