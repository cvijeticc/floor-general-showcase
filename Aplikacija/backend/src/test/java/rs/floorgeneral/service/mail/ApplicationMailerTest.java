package rs.floorgeneral.service.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import rs.floorgeneral.domain.Application;
import rs.floorgeneral.domain.SkillLevel;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.repository.ApplicationRepository;

/** D-20 — mejl ligi o novoj prijavi: kome ide, šta piše i šta se dešava kad SMTP nije podešen. */
class ApplicationMailerTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final ApplicationRepository repository = mock(ApplicationRepository.class);
    private final MailProperties properties =
            new MailProperties("liga@example.com, druga@example.com", "", "https://sajt.example/");

    @SuppressWarnings("unchecked")
    private ApplicationMailer mailer(JavaMailSender available) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(available);
        when(provider.getObject()).thenReturn(available);
        return new ApplicationMailer(provider, repository, properties, "smtp-nalog@example.com");
    }

    @Test
    void newApplication_isMailedToLeague_withReplyToApplicant() {
        when(repository.findById(7L)).thenReturn(Optional.of(application()));

        mailer(sender).onApplicationSubmitted(new ApplicationSubmittedEvent(7L));

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        SimpleMailMessage message = sent.getValue();
        assertThat(message.getTo()).containsExactly("liga@example.com", "druga@example.com");
        assertThat(message.getFrom()).isEqualTo("smtp-nalog@example.com");
        assertThat(message.getReplyTo()).isEqualTo("stele@example.com");
        assertThat(message.getSubject()).isEqualTo("Nova prijava: Stefan Stefanović „Stele“");
        assertThat(message.getText())
                .contains("+381 64 1234567", "192 cm", "88 kg", "1999.", "Trenirao sam pre", "Ne mogu nedeljom")
                .contains("https://sajt.example/admin/prijave")
                .contains("11.11.2026. u 18:30");
    }

    @Test
    void withoutSmtp_nothingIsSent_andTestMailExplainsWhy() {
        ApplicationMailer mailer = mailer(null);

        mailer.onApplicationSubmitted(new ApplicationSubmittedEvent(7L));

        verify(repository, never()).findById(any());
        assertThat(mailer.status().enabled()).isFalse();
        assertThatThrownBy(() -> mailer.sendTest("admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("SPRING_MAIL_HOST");
    }

    @Test
    void failingSmtp_doesNotThrowFromListener_butTestMailReportsIt() {
        when(repository.findById(7L)).thenReturn(Optional.of(application()));
        org.mockito.Mockito.doThrow(new MailSendException("Authentication failed"))
                .when(sender).send(any(SimpleMailMessage.class));
        ApplicationMailer mailer = mailer(sender);

        mailer.onApplicationSubmitted(new ApplicationSubmittedEvent(7L));

        assertThatThrownBy(() -> mailer.sendTest("admin"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Authentication failed");
    }

    @Test
    void status_listsRecipientsAndSender() {
        var status = mailer(sender).status();

        assertThat(status.enabled()).isTrue();
        assertThat(status.recipients()).containsExactly("liga@example.com", "druga@example.com");
        assertThat(status.sender()).isEqualTo("smtp-nalog@example.com");
    }

    private static Application application() {
        Application a = new Application();
        a.setApplicationID(7L);
        a.setFullName("Stefan Stefanović");
        a.setNickname("Stele");
        a.setPhone("+381 64 1234567");
        a.setEmail("stele@example.com");
        a.setHeightCm(192);
        a.setWeightKg(88);
        a.setBirthYear(1999);
        a.setSkillLevel(SkillLevel.TRAINED_BEFORE);
        a.setNote("Ne mogu nedeljom");
        a.setCreatedAt(LocalDateTime.of(2026, 11, 11, 18, 30));
        return a;
    }
}
