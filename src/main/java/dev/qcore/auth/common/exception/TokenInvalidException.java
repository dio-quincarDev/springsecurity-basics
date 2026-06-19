package dev.qcore.auth.common.exception;

import lombok.Getter;

// Excepción lanzada cuando un token JWT no es válido (expirado, firma incorrecta, malformado, etc.).
// El GlobalExceptionHandler la captura y la convierte en HTTP 401 con un TokenPayload
// que incluye el código de error (TOKEN_EXPIRED / INVALID_TOKEN) y el mensaje descriptivo.
@Getter
public class TokenInvalidException extends RuntimeException {
    private final String error;
    private final String message;

    public TokenInvalidException(String error, String message) {
        super(message);
        this.error = error;
        this.message = message;
    }
}
