package dev.qcore.auth.common.exception;

import dev.qcore.auth.common.constants.ErrorCodes;
import dev.qcore.auth.common.constants.ErrorMessages;
import dev.qcore.auth.common.model.dto.response.ErrorResponse;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return buildResponse(ErrorCodes.INVALID_CREDENTIALS, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse(ErrorCodes.INVALID_CREDENTIALS, ErrorMessages.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(ErrorCodes.ACCESS_DENIED, ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        return buildResponse(ErrorCodes.USER_ALREADY_EXISTS, ErrorMessages.USER_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }

    // Captura TokenInvalidException (lanzada por AuthServiceImpl cuando el token JWT no es válido)
    // y devuelve HTTP 401 con un TokenPayload que contiene el código de error y mensaje.
    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<TokenPayload> handleTokenInvalid(TokenInvalidException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new TokenPayload(false, null, null, null, ex.getError(), ex.getMessage()));
    }

    // Captura MissingRequestHeaderException (header requerido faltante, ej: Authorization)
    // y devuelve HTTP 400 con un ErrorResponse descriptivo.
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return buildResponse(ErrorCodes.VALIDATION_ERROR,
                "Header requerido faltante: " + ex.getHeaderName(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ErrorMessages.VALIDATION_FAILED);
        return buildResponse(ErrorCodes.VALIDATION_ERROR, message, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            String error, String message, HttpStatus status) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .error(error)
                .message(message)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(errorResponse);
    }

}
