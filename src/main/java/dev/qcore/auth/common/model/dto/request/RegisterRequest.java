package dev.qcore.auth.common.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Datos para registro de nuevo usuario")
public record RegisterRequest(
        @Schema(description = "Nombre de usuario único", example = "javier_ruiz")
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^\\S+$", message = "Username cannot contain spaces")
        String username,

        @Schema(description = "Correo electrónico único", example = "javier@email.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(description = "Password: mínimo 8 caracteres, 1 mayúscula, 1 número", example = "Password123")
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must contain at least one uppercase letter and one number")
        String password) {
}
