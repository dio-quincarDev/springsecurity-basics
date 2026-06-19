package dev.qcore.auth.config;

import dev.qcore.auth.common.model.entities.UserEntity;
import dev.qcore.auth.repository.UserEntityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserEntityRepository userEntityRepository;

    @Test
    void loadUserByUsername_withExistingEmail_returnsUserEntity() {
        var user = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded")
                .build();
        given(userEntityRepository.findByEmail("test@example.com"))
                .willReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userEntityRepository);
        var result = service.loadUserByUsername("test@example.com");

        assertThat(result).isEqualTo(user);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getPassword()).isEqualTo("encoded");
    }

    @Test
    void loadUserByUsername_withUnknownEmail_throwsUsernameNotFoundException() {
        given(userEntityRepository.findByEmail("unknown@example.com"))
                .willReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(userEntityRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }
}
