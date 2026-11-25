package rs.floorgeneral.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import rs.floorgeneral.exception.ApiException;
import rs.floorgeneral.exception.ResourceNotFoundException;
import rs.floorgeneral.web.dto.common.ApiError;

/**
 * Fokus: svaki izuzetak — poznat ili nepoznat — mora zavrsiti kao uredan ApiError, nikad kao
 * gola 500 stranica ili procureli stack trace u odgovoru.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiException_usesStatusAndMessageFromException() {
        when(request.getRequestURI()).thenReturn("/api/v1/leagues/404");
        ApiException ex = new ResourceNotFoundException("Liga sa ID 404 ne postoji");

        ResponseEntity<ApiError> response = handler.handleApiException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Liga sa ID 404 ne postoji");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/leagues/404");
    }

    @Test
    void handleDataIntegrity_returns409WithGenericMessage() {
        when(request.getRequestURI()).thenReturn("/api/v1/leagues/1");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("FK violation on leagues");

        ResponseEntity<ApiError> response = handler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).doesNotContain("FK violation");
    }

    @Test
    void handleUnexpected_returns500AndNeverLeaksExceptionMessage() {
        when(request.getRequestURI()).thenReturn("/api/v1/leagues");
        RuntimeException ex = new RuntimeException("lozinka=tajna123 stack trace detalji");

        ResponseEntity<ApiError> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).doesNotContain("tajna123");
        assertThat(response.getBody().message()).isEqualTo("Doslo je do neocekivane greske na serveru");
    }
}
