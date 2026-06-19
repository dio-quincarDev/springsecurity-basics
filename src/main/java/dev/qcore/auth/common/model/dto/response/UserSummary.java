package dev.qcore.auth.common.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumen de un usuario (solo para administradores)")
public record UserSummary(
        @Schema(description = "ID del usuario", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "Nombre de usuario", example = "javier_ruiz")
        String username,

        @Schema(description = "Correo electrónico", example = "javier@email.com")
        String email,

        @Schema(description = "Rol del usuario", example = "ADMIN")
        String role
) {
}
