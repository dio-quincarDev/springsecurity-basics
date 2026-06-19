package dev.qcore.auth.service.impl;

import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceImplTest {

    private static final String SECRET = "my-test-secret-key-that-is-at-least-32-characters!";
    private static final long EXPIRATION_MS = TimeUnit.HOURS.toMillis(1);

    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(SECRET, EXPIRATION_MS);
    }

    @Test
    void constructor_withShortSecret_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new JwtServiceImpl("too-short", EXPIRATION_MS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 32 caracteres");
    }

    @Test
    void generateToken_returnsTokenResponseWithExpectedFields() {
        TokenResponse response = jwtService.generateToken(
                "user@example.com", "550e8400-e29b-41d4-a716-446655440000", "testuser", "USER");

        assertThat(response.token()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(TimeUnit.MILLISECONDS.toSeconds(EXPIRATION_MS));
        assertThat(response.userId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    void validateToken_withValidToken_returnsValidPayload() {
        TokenResponse tokenRes = jwtService.generateToken(
                "user@example.com", "550e8400-e29b-41d4-a716-446655440000", "testuser", "USER");

        TokenPayload payload = jwtService.validateToken(tokenRes.token());

        assertThat(payload.valid()).isTrue();
        assertThat(payload.userId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        assertThat(payload.username()).isEqualTo("testuser");
        assertThat(payload.role()).isEqualTo("USER");
        assertThat(payload.error()).isNull();
        assertThat(payload.message()).isNull();
    }

    @Test
    void validateToken_withExpiredToken_returnsExpiredPayload() throws Exception {
        JwtServiceImpl shortLived = new JwtServiceImpl(SECRET, 1);
        TokenResponse tokenRes = shortLived.generateToken(
                "user@example.com", "id", "user", "USER");

        Thread.sleep(10);

        TokenPayload payload = shortLived.validateToken(tokenRes.token());

        assertThat(payload.valid()).isFalse();
        assertThat(payload.error()).isEqualTo("TOKEN_EXPIRED");
        assertThat(payload.message()).isEqualTo("The token has expired");
    }

    @Test
    void validateToken_withMalformedToken_returnsInvalidPayload() {
        TokenPayload payload = jwtService.validateToken("malformed.jwt.token");

        assertThat(payload.valid()).isFalse();
        assertThat(payload.error()).isEqualTo("INVALID_TOKEN");
        assertThat(payload.message()).isEqualTo("The token is invalid");
    }

    @Test
    void validateToken_withRandomString_returnsInvalidPayload() {
        TokenPayload payload = jwtService.validateToken("random-non-jwt-string");

        assertThat(payload.valid()).isFalse();
        assertThat(payload.error()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void generateToken_withRolePrefix_normalizesRole() {
        TokenResponse response = jwtService.generateToken(
                "user@example.com", "id", "user", "ROLE_ADMIN");

        TokenPayload payload = jwtService.validateToken(response.token());

        assertThat(payload.role()).isEqualTo("ADMIN");
    }

    @Test
    void isExpired_withValidToken_returnsFalse() {
        TokenResponse tokenRes = jwtService.generateToken(
                "user@example.com", "id", "user", "USER");

        boolean expired = jwtService.isExpired(tokenRes.token());

        assertThat(expired).isFalse();
    }

    @Test
    void isExpired_withExpiredToken_returnsTrue() throws Exception {
        JwtServiceImpl shortLived = new JwtServiceImpl(SECRET, 1);
        TokenResponse tokenRes = shortLived.generateToken(
                "user@example.com", "id", "user", "USER");

        Thread.sleep(10);

        boolean expired = shortLived.isExpired(tokenRes.token());

        assertThat(expired).isTrue();
    }

    @Test
    void isExpired_withMalformedToken_returnsTrue() {
        assertThat(jwtService.isExpired("bad-token")).isTrue();
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        TokenResponse tokenRes = jwtService.generateToken(
                "user@example.com", "id", "user", "USER");

        String email = jwtService.extractEmail(tokenRes.token());

        assertThat(email).isEqualTo("user@example.com");
    }

    @Test
    void extractRole_returnsCorrectRole() {
        TokenResponse tokenRes = jwtService.generateToken(
                "user@example.com", "id", "user", "ADMIN");

        String role = jwtService.extractRole(tokenRes.token());

        assertThat(role).isEqualTo("ADMIN");
    }
}
