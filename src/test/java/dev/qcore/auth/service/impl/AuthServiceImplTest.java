package dev.qcore.auth.service.impl;

import dev.qcore.auth.common.exception.DuplicateEmailException;
import dev.qcore.auth.common.exception.TokenInvalidException;
import dev.qcore.auth.common.model.dto.request.LoginRequest;
import dev.qcore.auth.common.model.dto.request.RegisterRequest;
import dev.qcore.auth.common.model.dto.response.RegisterResponse;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import dev.qcore.auth.common.model.entities.UserEntity;
import dev.qcore.auth.common.model.mapper.UserMapper;
import dev.qcore.auth.repository.UserEntityRepository;
import dev.qcore.auth.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserEntityRepository userEntityRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;

    @Captor
    private ArgumentCaptor<UserEntity> userCaptor;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userEntityRepository, passwordEncoder, jwtService,
                authenticationManager, userMapper);
    }

    @Test
    void createUser_withValidRequest_returnsRegisterResponse() {
        var request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123")
                .build();
        var mappedEntity = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPass")
                .role(dev.qcore.auth.common.enums.UserRole.USER)
                .build();
        var savedEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encodedPass")
                .role(dev.qcore.auth.common.enums.UserRole.USER)
                .createdAt(Instant.now())
                .build();

        given(userEntityRepository.findByEmail("test@example.com")).willReturn(Optional.empty());
        given(userMapper.toUserEntity(request)).willReturn(mappedEntity);
        given(passwordEncoder.encode("Password123")).willReturn("encodedPass");
        given(userEntityRepository.save(any(UserEntity.class))).willReturn(savedEntity);

        RegisterResponse response = authService.createUser(request);

        assertThat(response.id()).isEqualTo(savedEntity.getId().toString());
        assertThat(response.username()).isEqualTo("testuser");
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.createdAt()).isEqualTo(savedEntity.getCreatedAt());

        then(userEntityRepository).should().findByEmail("test@example.com");
        then(userEntityRepository).should().save(userCaptor.capture());
        UserEntity captured = userCaptor.getValue();
        assertThat(captured.getPassword()).isEqualTo("encodedPass");
        assertThat(captured.getCreatedAt()).isNotNull();
    }

    @Test
    void createUser_withDuplicateEmail_throwsDuplicateEmailException() {
        var request = RegisterRequest.builder()
                .username("testuser")
                .email("existing@example.com")
                .password("Password123")
                .build();
        var existingUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .build();

        given(userEntityRepository.findByEmail("existing@example.com"))
                .willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.createUser(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("USER_ALREADY_EXISTS");
    }

    @Test
    void login_withValidCredentials_returnsTokenResponse() {
        var request = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123")
                .build();
        var user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .username("testuser")
                .role(dev.qcore.auth.common.enums.UserRole.USER)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(user, null);
        var tokenResponse = TokenResponse.builder()
                .token("jwt-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(user.getId().toString())
                .build();

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(auth);
        given(jwtService.generateToken("test@example.com", user.getId().toString(), "testuser", "USER"))
                .willReturn(tokenResponse);

        TokenResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.userId()).isEqualTo(user.getId().toString());
    }

    @Test
    void login_withInvalidCredentials_throwsException() {
        var request = LoginRequest.builder()
                .email("wrong@example.com")
                .password("WrongPass1")
                .build();

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void validateToken_withValidToken_returnsPayload() {
        var tokenPayload = new TokenPayload(true, "user-id", "testuser", "USER", null, null);

        given(jwtService.validateToken("some-token")).willReturn(tokenPayload);

        TokenPayload result = authService.validateToken("Bearer some-token");

        assertThat(result.valid()).isTrue();
        assertThat(result.userId()).isEqualTo("user-id");
        assertThat(result.username()).isEqualTo("testuser");
        then(jwtService).should().validateToken("some-token");
    }

    @Test
    void validateToken_withInvalidToken_throwsTokenInvalidException() {
        var tokenPayload = new TokenPayload(false, null, null, null, "TOKEN_EXPIRED", "El token ha expirado");

        given(jwtService.validateToken("some-token")).willReturn(tokenPayload);

        assertThatThrownBy(() -> authService.validateToken("Bearer some-token"))
                .isInstanceOf(TokenInvalidException.class)
                .hasMessage("El token ha expirado");
    }
}
