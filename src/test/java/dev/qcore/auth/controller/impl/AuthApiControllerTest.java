package dev.qcore.auth.controller.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.qcore.auth.common.constants.ApiPaths;
import dev.qcore.auth.common.constants.HeaderConstants;
import dev.qcore.auth.common.model.dto.request.LoginRequest;
import dev.qcore.auth.common.model.dto.request.RegisterRequest;
import dev.qcore.auth.common.model.dto.response.RegisterResponse;
import dev.qcore.auth.common.model.dto.response.TokenPayload;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import dev.qcore.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthApiControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthApiController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createUser_withValidRequest_returns201() throws Exception {
        var request = RegisterRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .password("Password123")
                .build();
        var response = new RegisterResponse(
                UUID.randomUUID().toString(), "testuser", "test@example.com", "USER", Instant.now());

        given(authService.createUser(any(RegisterRequest.class))).willReturn(response);

        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post(ApiPaths.REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.id()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void createUser_withInvalidBody_returns400() throws Exception {
        var request = RegisterRequest.builder()
                .username("")
                .email("not-an-email")
                .password("short")
                .build();

        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post(ApiPaths.REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        var request = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123")
                .build();
        var response = TokenResponse.builder()
                .token("jwt-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .userId(UUID.randomUUID().toString())
                .build();

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post(ApiPaths.LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_withInvalidBody_returns400() throws Exception {
        var request = LoginRequest.builder()
                .email("")
                .password("")
                .build();

        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post(ApiPaths.LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateToken_withValidToken_returns200() throws Exception {
        var payload = new TokenPayload(true, "user-id", "testuser", "USER", null, null);

        given(authService.validateToken("Bearer valid-jwt-token")).willReturn(payload);

        var mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(post(ApiPaths.VALIDATE_ENDPOINT)
                        .header(HeaderConstants.AUTHORIZATION, "Bearer valid-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").value("user-id"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
}
