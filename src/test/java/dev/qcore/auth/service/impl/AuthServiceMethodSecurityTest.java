package dev.qcore.auth.service.impl;

import dev.qcore.auth.common.enums.UserRole;
import dev.qcore.auth.common.model.dto.response.UserSummary;
import dev.qcore.auth.common.model.entities.UserEntity;
import dev.qcore.auth.repository.UserEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest
class AuthServiceMethodSecurityTest {

    @Autowired
    private AuthServiceImpl authService;

    @MockitoBean
    private UserEntityRepository userEntityRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_asAdmin_returnsUserList() {
        var user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .email("admin@example.com")
                .password("encoded")
                .role(UserRole.ADMIN)
                .build();
        given(userEntityRepository.findAll()).willReturn(List.of(user));

        List<UserSummary> result = authService.listUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("admin");
        assertThat(result.get(0).role()).isEqualTo("ADMIN");
    }

    @Test
    @WithMockUser(roles = "USER")
    void listUsers_asUser_throwsAccessDenied() {
        assertThatThrownBy(() -> authService.listUsers())
                .isInstanceOf(AccessDeniedException.class);
    }
}
