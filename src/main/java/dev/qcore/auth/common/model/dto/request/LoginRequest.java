package dev.qcore.auth.common.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(
        @Schema(description = "Correo electrónico registrado", example = "javier@email.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Contraseña del usuario", example = "Password123")
        @NotBlank(message = "Password is required")
        String password
) {
}
