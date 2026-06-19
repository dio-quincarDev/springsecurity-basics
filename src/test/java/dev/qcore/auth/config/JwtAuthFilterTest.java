package dev.qcore.auth.config;

import dev.qcore.auth.common.constants.HeaderConstants;
import dev.qcore.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(jwtService, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_withoutAuthHeader_passesThrough() throws Exception {
        given(request.getHeader(HeaderConstants.AUTHORIZATION)).willReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withNonBearerHeader_passesThrough() throws Exception {
        given(request.getHeader(HeaderConstants.AUTHORIZATION)).willReturn("Basic some-token");

        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withEmptyAuthHeader_passesThrough() throws Exception {
        given(request.getHeader(HeaderConstants.AUTHORIZATION)).willReturn("");

        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withInvalidJwt_passesThrough() throws Exception {
        given(request.getHeader(HeaderConstants.AUTHORIZATION)).willReturn("Bearer bad-token");
        given(jwtService.extractEmail("bad-token")).willThrow(new RuntimeException("Invalid JWT"));

        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withExpiredJwt_passesThrough() throws Exception {
        given(request.getHeader(HeaderConstants.AUTHORIZATION)).willReturn("Bearer expired-token");
        given(jwtService.extractEmail("expired-token")).willReturn("user@example.com");
        var userDetails = new User("user@example.com", "pass", List.of());
        given(userDetailsService.loadUserByUsername("user@example.com")).willReturn(userDetails);
        given(jwtService.isExpired("expired-token")).willReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        then(filterChain).should().doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doFilterInternal_withValidJwt_setsAuthentication() throws Exception {
        given(request.getHeader(HeaderConstants.AUTHORIZATION)).willReturn("Bearer valid-token");
        given(jwtService.extractEmail("valid-token")).willReturn("user@example.com");
        var userDetails = new User("user@example.com", "pass", List.of());
        given(userDetailsService.loadUserByUsername("user@example.com")).willReturn(userDetails);
        given(jwtService.isExpired("valid-token")).willReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        assertThat(auth.getCredentials()).isNull();
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void doFilterInternal_withExistingAuthentication_doesNotOverride() throws Exception {
        var existingAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "existing", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existingAuth);
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void doFilterInternal_whenUserNotFound_propagatesException() {
        given(request.getHeader(HeaderConstants.AUTHORIZATION)).willReturn("Bearer valid-token");
        given(jwtService.extractEmail("valid-token")).willReturn("unknown@example.com");
        given(userDetailsService.loadUserByUsername("unknown@example.com"))
                .willThrow(new UsernameNotFoundException("User not found"));

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
