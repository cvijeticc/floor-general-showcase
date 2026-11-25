package rs.floorgeneral.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RSA par kljuceva za potpisivanje JWT-a se generise pri svakom pokretanju aplikacije.
 * Posledica: izdati tokeni ne prezivljavaju restart/skaliranje na vise instanci — prihvatljivo
 * za MVP sa jednim admin nalogom. Kad se pojavi potreba za stabilnim kljucem (npr. vise instanci
 * u produkciji), ovaj bean se zamenjuje ucitavanjem kljuca iz konfiguracije/secret managera,
 * bez izmene ostatka security sloja.
 */
@Configuration
public class RsaKeyConfig {

    @Bean
    public KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Ne mogu da generisem RSA par kljuceva za JWT", e);
        }
    }

    @Bean
    public RSAPublicKey rsaPublicKey(KeyPair rsaKeyPair) {
        return (RSAPublicKey) rsaKeyPair.getPublic();
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey(KeyPair rsaKeyPair) {
        return (RSAPrivateKey) rsaKeyPair.getPrivate();
    }
}
