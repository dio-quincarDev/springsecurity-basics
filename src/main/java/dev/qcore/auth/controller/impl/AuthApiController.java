package dev.qcore.auth.controller.impl;

import dev.qcore.auth.common.constants.HeaderConstants;
import dev.qcore.auth.common.model.dto.request.LoginRequest;
import dev.qcore.auth.common.model.dto.request.RegisterRequest;
import dev.qcore.auth.common.model.dto.response.RegisterResponse;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import dev.qcore.auth.common.model.dto.response.UserSummary;
import dev.qcore.auth.controller.AuthApi;
import dev.qcore.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

// Controlador REST que implementa los endpoints de AuthApi.
// NO contiene lógica de negocio ni decisiones sobre códigos HTTP —
// solo recibe la petición, delega en AuthService y devuelve la respuesta.
@RestController
@RequiredArgsConstructor
public class AuthApiController implements AuthApi {
    private final AuthService authService;

    @Override
    public ResponseEntity<RegisterResponse> createUser(@RequestBody @Valid RegisterRequest registerRequest) {
        RegisterResponse response = authService.createUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        TokenResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    // Recibe el header Authorization completo (incluyendo "Bearer ") y lo pasa al servicio.
    // Si el token es inválido, AuthServiceImpl lanza TokenInvalidException
    // y el GlobalExceptionHandler devuelve 401 automáticamente.
    @Override
    public ResponseEntity<TokenPayload> validateToken(@RequestHeader(HeaderConstants.AUTHORIZATION) String authHeader) {
        return ResponseEntity.ok(authService.validateToken(authHeader));
    }

    // Requiere rol ADMIN (la anotación @PreAuthorize está en AuthApi y AuthServiceImpl)
    @Override
    public ResponseEntity<List<UserSummary>> listUsers() {
        return ResponseEntity.ok(authService.listUsers());
    }
}