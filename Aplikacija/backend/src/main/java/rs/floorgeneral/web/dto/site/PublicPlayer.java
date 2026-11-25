package rs.floorgeneral.web.dto.site;

/** Igrac onako kako sme da se objavi: ime i nadimak. Telefon, uplata i napomena ne izlaze. */
public record PublicPlayer(
        Long id,
        String name,
        String nickname
) {
}
