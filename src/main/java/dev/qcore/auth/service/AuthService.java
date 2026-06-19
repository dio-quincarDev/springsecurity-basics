package dev.qcore.auth.service;

import dev.qcore.auth.common.model.dto.request.LoginRequest;
import dev.qcore.auth.common.model.dto.request.RegisterRequest;
import dev.qcore.auth.common.model.dto.response.RegisterResponse;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import dev.qcore.auth.common.model.dto.response.UserSummary;

import java.util.List;

public interface AuthService {
    RegisterResponse createUser (RegisterRequest registerRequest);
    TokenResponse login (LoginRequest loginRequest);
    TokenPayload validateToken(String authHeader);
    List<UserSummary> listUsers();
}
