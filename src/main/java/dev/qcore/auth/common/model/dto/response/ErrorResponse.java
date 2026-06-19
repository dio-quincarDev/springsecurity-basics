package dev.qcore.auth.common.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;

@Builder
@Schema(description = "Estructura estándar de error de la API")
public record ErrorResponse(
        @Schema(description = "Código de error legible por máquina", example = "USER_ALREADY_EXISTS")
        String error,

        @Schema(description = "Mensaje descriptivo del error", example = "Ya existe un usuario con ese correo")
        String message,

        @Schema(description = "Marca de tiempo del error", example = "2025-01-15T10:30:00Z")
        Instant timestamp){
}
