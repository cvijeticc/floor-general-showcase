package rs.floorgeneral.exception;

import org.springframework.http.HttpStatus;

/** Bazni neproveravani izuzetak za poslovne/API greske koje same nose svoj HTTP status. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
