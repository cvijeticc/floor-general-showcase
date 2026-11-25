package rs.floorgeneral.service.mail;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Mejl o prijavi se šalje u pozadini — posetilac ne čeka SMTP server da bi video potvrdu. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
