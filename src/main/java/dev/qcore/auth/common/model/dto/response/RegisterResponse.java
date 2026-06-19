package dev.qcore.auth.common.model.dto.response;


import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Respuesta del registro de usuario")
public record RegisterResponse(
        @Schema(description = "ID del usuario creado", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Nombre de usuario", example = "javier_ruiz")
        String username,

        @Schema(description = "Correo electrónico", example = "javier@email.com")
        String email,

        @Schema(description = "Rol del usuario", example = "USER")
        String role,

        @Schema(description = "Fecha y hora de creación", example = "2025-01-15T10:30:00Z")
        Instant createdAt
) {
}
