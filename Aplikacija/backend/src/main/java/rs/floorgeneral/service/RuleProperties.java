package rs.floorgeneral.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametri pravila koja nisu deo formata takmicenja, pa im nije mesto na entitetu sezone:
 * disciplinski prag iz BR-10.4 (jos nije potvrdjen kod narucioca) i minimalna duzina lozinke.
 */
@ConfigurationProperties(prefix = "app.rules")
public record RuleProperties(
        Integer walkoverWarningThreshold,
        Integer minPasswordLength
) {

    public int walkoverWarningThresholdOrDefault() {
        return walkoverWarningThreshold == null ? 2 : walkoverWarningThreshold;
    }

    public int minPasswordLengthOrDefault() {
        return minPasswordLength == null ? 8 : minPasswordLength;
    }
}
