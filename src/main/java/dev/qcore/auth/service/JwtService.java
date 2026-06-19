package dev.qcore.auth.service;

import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import io.jsonwebtoken.Claims;

public interface JwtService {

    TokenResponse generateToken(String email, String userId, String username, String role);

    TokenPayload validateToken(String token);

    Claims getClaims(String token);

    boolean isExpired(String token);

    String extractRole(String token);

    String extractEmail(String token);


}
