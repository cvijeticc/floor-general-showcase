package rs.floorgeneral.service.mail;

/** Nova prijava je upisana — mejl se šalje tek kad transakcija prođe (D-20). */
public record ApplicationSubmittedEvent(Long applicationId) {
}
