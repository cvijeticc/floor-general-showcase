package rs.floorgeneral.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * PMOV: Korisnik. Tabela se zove "app_users" jer je "user" rezervisana rec u PostgreSQL-u.
 * U MVP-u se prijavljuje samo administrator.
 */
@Entity
@Table(name = "app_users")
@Getter
@Setter
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "app_user_id")
    private Long appUserID;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    /** Cuva se iskljucivo BCrypt hash, nikad otvoren tekst. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.ADMIN;
}
