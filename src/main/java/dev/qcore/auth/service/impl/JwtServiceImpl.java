package dev.qcore.auth.service.impl;

import dev.qcore.auth.common.constants.ErrorCodes;
import dev.qcore.auth.common.constants.ErrorMessages;
import dev.qcore.auth.common.constants.JwtConstants;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import dev.qcore.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class JwtServiceImpl implements JwtService {

        private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);
        private final SecretKey secretKey;
        private final long expirationTime;

        public JwtServiceImpl(
                @Value("${jwt.secret}") String secret,
                @Value("${jwt.access-expiration}") long accessExpiration) {
            // La clave secreta debe tener al menos 32 bytes (256 bits) porque usamos HMAC-SHA.
            // Menos de 32 bytes es inseguro y la librería jjwt lo rechazaría.
            if (secret.getBytes().length < 32) {
                throw new IllegalArgumentException("La clave secreta de JWT debe tener al menos 32 caracteres.");
            }
            this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            this.expirationTime = accessExpiration;
        }

        @Override
        public TokenResponse generateToken(String email, String userId, String username, String role) {
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + expirationTime);

            // Si el rol tiene prefijo "ROLE_" (ej: "ROLE_ADMIN"), lo normalizamos quitándolo.
            // Spring Security usa el prefijo internamente, pero en el JWT guardamos el valor limpio.
            String normalizedRole = role.startsWith(JwtConstants.ROLE_PREFIX)
                    ? role.substring(JwtConstants.ROLE_PREFIX.length())
                    : role;

            // Construimos el JWT con los datos del usuario y lo firmamos con HMAC-SHA.
            // - subject: email del usuario (identificador único)
            // - claims: userId, username, role (datos adicionales)
            // - issuedAt: momento de emisión
            // - expiration: momento de expiración
            // - signWith: firma digital usando la clave secreta (HMAC-SHA)
            // El resultado es un string como "eyJhbGciOiJIUzI1NiJ9..."
            String token = Jwts.builder()
                    .subject(email)
                    .claim(JwtConstants.CLAIM_USER_ID, userId)
                    .claim(JwtConstants.CLAIM_USERNAME, username)
                    .claim(JwtConstants.CLAIM_ROLE, normalizedRole)
                    .issuedAt(now)
                    .expiration(expirationDate)
                    .signWith(secretKey)
                    .compact();

            return TokenResponse.builder()
                    .token(token)
                    .tokenType(JwtConstants.TOKEN_TYPE_BEARER)
                    .expiresIn(TimeUnit.MILLISECONDS.toSeconds(expirationTime))
                    .userId(userId)
                    .build();
        }

        @Override
        public TokenPayload validateToken(String token) {
            try {
                // Parsea el token: verifica la firma (con la clave secreta) y extrae los claims
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Verifica si el token ya expiró comparando la fecha de expiración con la actual
                if (claims.getExpiration().before(new Date())) {
                    return new TokenPayload(false, null, null, null, ErrorCodes.TOKEN_EXPIRED, ErrorMessages.TOKEN_EXPIRED);
                }

                // Token válido: devolvemos los datos del usuario
                return new TokenPayload(
                        true,
                        claims.get(JwtConstants.CLAIM_USER_ID, String.class),
                        claims.get(JwtConstants.CLAIM_USERNAME, String.class),
                        claims.get(JwtConstants.CLAIM_ROLE, String.class),
                        null,
                        null
                );
            } catch (ExpiredJwtException e) {
                // Token expirado: devolvemos error específico para que el cliente sepa que debe renovar
                return new TokenPayload(false, null, null, null, ErrorCodes.TOKEN_EXPIRED, ErrorMessages.TOKEN_EXPIRED);
            } catch (Exception e) {
                // Cualquier otro error: firma inválida, token malformado, etc.
                log.warn("Token JWT inválido: {}", e.getMessage());
                return new TokenPayload(false, null, null, null, ErrorCodes.INVALID_TOKEN, ErrorMessages.INVALID_TOKEN);
            }
        }

        @Override
        public Claims getClaims(String token) {
            // Método helper: parsea el token y devuelve los claims (datos internos).
            // Si el token es inválido, lanza IllegalArgumentException.
            try {
                return Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception e) {
                log.error("Error al parsear JWT: {}, Causa: {}", e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "N/A");
                throw new IllegalArgumentException("Token JWT inválido o expirado", e);
            }
        }

        @Override
        public boolean isExpired(String token) {
            // Compara la fecha de expiración del token con la fecha actual.
            // Si hay cualquier error al parsear, asumimos que está expirado.
            try {
                return getClaims(token).getExpiration().before(new Date());
            } catch (Exception e) {
                return true;
            }
        }

        @Override
        public String extractRole(String token) {
            return getClaims(token).get(JwtConstants.CLAIM_ROLE, String.class);
        }

        @Override
        public String extractEmail(String token) {
            return getClaims(token).getSubject();
        }
}
