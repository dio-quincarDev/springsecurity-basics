package dev.qcore.auth.common.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Token JWT para autenticación")
public record TokenResponse(
        @Schema(description = "Token JWT para usar en header Authorization",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,
        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,
        @Schema(description = "Tiempo de expiración del token en segundos", example = "3600")
        long expiresIn,
        @Schema(description = "ID del usuario autenticado",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String userId
) {
}
