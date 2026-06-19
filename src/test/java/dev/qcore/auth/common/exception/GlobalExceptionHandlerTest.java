package dev.qcore.auth.common.exception;

import dev.qcore.auth.common.model.dto.response.ErrorResponse;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleInvalidCredentials_returns401() {
        var ex = new InvalidCredentialsException("Credenciales inválidas");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().message()).isEqualTo("Credenciales inválidas");
    }

    @Test
    void handleBadCredentials_returns401() {
        var ex = new BadCredentialsException("bad credentials");

        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleAccessDenied_returns403() {
        var ex = new AccessDeniedException("Acceso denegado");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().message()).isEqualTo("Acceso denegado");
    }

    @Test
    void handleDuplicateEmail_returns409() {
        var ex = new DuplicateEmailException("USER_ALREADY_EXISTS");

        ResponseEntity<ErrorResponse> response = handler.handleDuplicateEmail(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("USER_ALREADY_EXISTS");
        assertThat(response.getBody().message()).isEqualTo("A user with that email already exists");
    }

    @Test
    void handleTokenInvalid_returns401WithTokenPayload() {
        var ex = new TokenInvalidException("TOKEN_EXPIRED", "El token ha expirado");

        ResponseEntity<TokenPayload> response = handler.handleTokenInvalid(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().valid()).isFalse();
        assertThat(response.getBody().error()).isEqualTo("TOKEN_EXPIRED");
        assertThat(response.getBody().message()).isEqualTo("El token ha expirado");
    }

    @Test
    void handleValidationErrors_returns400() {
        var ex = createValidationException("email", "must be a well-formed email address");

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).contains("email").contains("well-formed");
    }

    private MethodArgumentNotValidException createValidationException(String field, String message) {
        var mockFieldError = new org.springframework.validation.FieldError("obj", field, message);
        BindingResult bindingResult = new org.springframework.validation.BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(mockFieldError);
        return new MethodArgumentNotValidException(null, bindingResult);
    }
}
