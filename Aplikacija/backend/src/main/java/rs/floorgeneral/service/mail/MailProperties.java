package rs.floorgeneral.service.mail;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * D-20 — kome i od koga ide mejl o novoj prijavi. SMTP server je u {@code spring.mail.*};
 * ovde je samo adresa lige (može ih biti više, odvojenih zarezom).
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String applicationsTo, String from, String siteUrl) {

    public List<String> recipients() {
        if (applicationsTo == null) {
            return List.of();
        }
        return Arrays.stream(applicationsTo.split(","))
                .map(String::trim)
                .filter(address -> !address.isEmpty())
                .toList();
    }
}
