package rs.floorgeneral.service.mail;

import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import rs.floorgeneral.domain.Application;
import rs.floorgeneral.domain.SkillLevel;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.repository.ApplicationRepository;
import rs.floorgeneral.web.dto.application.MailStatusInfo;

/**
 * D-20 — mejl ligi za svaku novu prijavu sa sajta (kao što je radio Framer). Baza je i dalje izvor
 * istine: ako SMTP nije podešen ili mejl ne prođe, prijava ostaje upisana i vidi se u adminu.
 *
 * <p>Šalje se tek posle commit-a i u pozadini, pa spor ili nedostupan SMTP server ne usporava
 * formu. Reply-To je adresa igrača — „Odgovori" u pošti piše direktno njemu.
 */
@Slf4j
@Service
public class ApplicationMailer {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd.MM.yyyy. 'u' HH:mm");

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ApplicationRepository applicationRepository;
    private final MailProperties properties;
    private final String smtpUsername;

    public ApplicationMailer(ObjectProvider<JavaMailSender> mailSender,
                             ApplicationRepository applicationRepository,
                             MailProperties properties,
                             @Value("${spring.mail.username:}") String smtpUsername) {
        this.mailSender = mailSender;
        this.applicationRepository = applicationRepository;
        this.properties = properties;
        this.smtpUsername = smtpUsername;
    }

    public MailStatusInfo status() {
        return new MailStatusInfo(isEnabled(), properties.recipients(), sender());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        if (!isEnabled()) {
            log.info("Prijava {} upisana; mejl nije poslat jer SMTP nije podešen", event.applicationId());
            return;
        }
        applicationRepository.findById(event.applicationId()).ifPresent(application -> {
            try {
                mailSender.getObject().send(newApplicationMessage(application));
                log.info("Mejl o prijavi {} poslat na {}", application.getApplicationID(), properties.recipients());
            } catch (MailException e) {
                log.error("Mejl o prijavi {} nije poslat: {}", application.getApplicationID(), e.getMessage(), e);
            }
        });
    }

    /** „Pošalji probni mejl" iz admina — odmah, da se vidi da li podešavanje radi. */
    public void sendTest(String requestedBy) {
        if (!isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Slanje mejlova nije podešeno na serveru (nedostaje SPRING_MAIL_HOST)");
        }
        SimpleMailMessage message = base();
        message.setSubject("Floor General — probni mejl");
        message.setText("""
                Ovo je probni mejl sa sajta Floor General lige.

                Ako ga čitaš, obaveštenja o novim prijavama stižu na ovu adresu.
                Poslao: %s
                """.formatted(requestedBy == null ? "admin" : requestedBy));
        try {
            mailSender.getObject().send(message);
        } catch (MailException e) {
            log.error("Probni mejl nije poslat: {}", e.getMessage(), e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Mejl nije poslat: " + e.getMessage());
        }
    }

    SimpleMailMessage newApplicationMessage(Application a) {
        SimpleMailMessage message = base();
        message.setReplyTo(a.getEmail());
        message.setSubject("Nova prijava: " + a.getFullName() + " „" + a.getNickname() + "“");
        message.setText("""
                Stigla je nova prijava u ligu.

                Ime i prezime:     %s
                Nadimak:           %s
                Telefon:           %s
                Email:             %s
                Visina:            %s
                Težina:            %s
                Godište:           %s
                Košarkaško znanje: %s

                Napomene:
                %s

                Nešto više o sebi:
                %s

                Poslato: %s
                Sve prijave: %s/admin/prijave
                """.formatted(
                a.getFullName(),
                a.getNickname(),
                a.getPhone(),
                a.getEmail(),
                orDash(a.getHeightCm(), " cm"),
                orDash(a.getWeightKg(), " kg"),
                orDash(a.getBirthYear(), "."),
                skillLabel(a.getSkillLevel()),
                a.getNote() == null ? "—" : a.getNote(),
                a.getAbout() == null ? "—" : a.getAbout(),
                a.getCreatedAt() == null ? "—" : WHEN.format(a.getCreatedAt()),
                trimSlash(properties.siteUrl())));
        return message;
    }

    private SimpleMailMessage base() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender());
        message.setTo(properties.recipients().toArray(String[]::new));
        return message;
    }

    private boolean isEnabled() {
        return mailSender.getIfAvailable() != null && !properties.recipients().isEmpty();
    }

    private String sender() {
        if (properties.from() != null && !properties.from().isBlank()) {
            return properties.from();
        }
        return smtpUsername == null || smtpUsername.isBlank() ? null : smtpUsername;
    }

    private static String skillLabel(SkillLevel level) {
        if (level == null) {
            return "—";
        }
        return switch (level) {
            case BEGINNER -> "Početnik";
            case TRAINED_BEFORE -> "Trenirao sam pre";
            case PRO -> "Profi";
        };
    }

    private static String orDash(Integer value, String unit) {
        return value == null ? "—" : value + unit;
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
