package rs.floorgeneral.web.dto.application;

import java.util.List;

/**
 * Da li se o novim prijavama šalje mejl i kome — admin to vidi na /admin/prijave.
 * {@code enabled = false} znači da SMTP nije podešen (nema SPRING_MAIL_HOST).
 */
public record MailStatusInfo(boolean enabled, List<String> recipients, String sender) {
}
