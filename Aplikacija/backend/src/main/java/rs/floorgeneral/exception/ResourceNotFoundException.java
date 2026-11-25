package rs.floorgeneral.exception;

import org.springframework.http.HttpStatus;

/** Baca se iz servisnog sloja kad trazeni entitet ne postoji u bazi. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
