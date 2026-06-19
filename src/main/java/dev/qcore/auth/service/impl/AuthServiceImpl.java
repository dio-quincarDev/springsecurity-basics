package dev.qcore.auth.service.impl;

import dev.qcore.auth.common.constants.ErrorCodes;
import dev.qcore.auth.common.constants.JwtConstants;
import dev.qcore.auth.common.exception.DuplicateEmailException;
import dev.qcore.auth.common.exception.TokenInvalidException;
import dev.qcore.auth.common.model.dto.request.LoginRequest;
import dev.qcore.auth.common.model.dto.request.RegisterRequest;
import dev.qcore.auth.common.model.dto.response.RegisterResponse;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import dev.qcore.auth.common.model.dto.response.UserSummary;
import dev.qcore.auth.common.model.entities.UserEntity;
import dev.qcore.auth.common.model.mapper.UserMapper;
import dev.qcore.auth.repository.UserEntityRepository;
import dev.qcore.auth.service.AuthService;
import dev.qcore.auth.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserEntityRepository userEntityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Override
    public RegisterResponse createUser(@Valid RegisterRequest registerRequest) {
        log.info("Intentando crear usuario para email: {}", registerRequest.email());

        // 1. Verificar que el email no esté registrado. Si ya existe, lanzamos excepción
        //    que el GlobalExceptionHandler convierte en HTTP 409 Conflict.
        if (userEntityRepository.findByEmail(registerRequest.email()).isPresent()) {
            log.warn("Intento de crear usuario con email existente: {}", registerRequest.email());
            throw new DuplicateEmailException(ErrorCodes.USER_ALREADY_EXISTS);
        }

        // 2. Convertir el DTO de request a entidad (UserMapper asigna rol USER por defecto)
        UserEntity userToSave = userMapper.toUserEntity(registerRequest);

        // 3. NUNCA guardamos la contraseña en texto plano.
        //    BCrypt la hashea con una "sal" aleatoria antes de persistirla.
        //    Así, aunque alguien acceda a la BD, no puede obtener la contraseña original.
        userToSave.setPassword(passwordEncoder.encode(registerRequest.password()));
        userToSave.setCreatedAt(Instant.now());

        // 4. Guardar en base de datos
        UserEntity userCreated = userEntityRepository.save(userToSave);
        log.info("Usuario creado exitosamente con ID: {}", userCreated.getId());

        // 5. Devolver los datos del usuario creado (sin la contraseña, obviamente)
        return new RegisterResponse(
                userCreated.getId().toString(),
                userCreated.getUsername(),
                userCreated.getEmail(),
                userCreated.getRole().name(),
                userCreated.getCreatedAt());
    }

    @Override
    public TokenResponse login(@Valid LoginRequest loginRequest) {
        log.info("Intentando login para usuario: {}", loginRequest.email());

        // 1. authenticationManager.authenticate() delega en DaoAuthenticationProvider
        //    (configurado en SecurityConfig), que:
        //    a) Llama a CustomUserDetailsService.loadUserByUsername() para buscar el usuario
        //    b) Usa BCryptPasswordEncoder.matches() para verificar la contraseña
        //    Si algo falla, lanza BadCredentialsException → GlobalExceptionHandler → 401
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.email(),
                        loginRequest.password()
                )
        );

        // 2. Si llegamos aquí, la autenticación fue exitosa.
        //    Obtenemos el usuario autenticado del objeto Authentication.
        UserEntity user = (UserEntity) authentication.getPrincipal();

        // 3. Generamos un JWT con los datos del usuario.
        //    El cliente usará este token en adelante en el header Authorization.
        log.info("Login exitoso para usuario: {}", user.getEmail());
        return jwtService.generateToken(
                user.getEmail(),
                user.getId().toString(),
                user.getUsername(),
                user.getRole().name());
    }

    @Override
    public TokenPayload validateToken(String authHeader) {
        // Extrae el token del header "Authorization" quitando el prefijo "Bearer "
        String token = authHeader.replace(JwtConstants.BEARER_PREFIX, "");
        // Delega en JwtService que verifica la firma y la expiración del token.
        TokenPayload payload = jwtService.validateToken(token);
        // Si el token no es válido, lanza excepción que el GlobalExceptionHandler convierte en 401
        if (!payload.valid()) {
            throw new TokenInvalidException(payload.error(), payload.message());
        }
        return payload;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    // @PreAuthorize (gracias a @EnableMethodSecurity en SecurityConfig) verifica automáticamente
    // que el usuario autenticado tenga el rol ADMIN antes de ejecutar este método.
    // Si el usuario tiene rol USER, Spring Security lanza AccessDeniedException → 403.
    // Si no está autenticado, lanza AuthenticationException → 401.
    public List<UserSummary> listUsers() {
        return userEntityRepository.findAll().stream()
                .map(u -> new UserSummary(
                        u.getId().toString(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getRole().name()))
                .toList();
    }
}