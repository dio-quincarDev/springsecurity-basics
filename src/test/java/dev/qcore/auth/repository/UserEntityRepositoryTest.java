package dev.qcore.auth.repository;

import dev.qcore.auth.common.enums.UserRole;
import dev.qcore.auth.common.model.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserEntityRepositoryTest {

    @Autowired
    private UserEntityRepository userEntityRepository;

    @Test
    void save_and_findByEmail_persistsAndRetrievesUser() {
        var user = UserEntity.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encoded-password")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        UserEntity saved = userEntityRepository.save(user);

        assertThat(saved.getId()).isNotNull();

        var found = userEntityRepository.findByEmail("test@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getPassword()).isEqualTo("encoded-password");
        assertThat(found.get().getRole()).isEqualTo(UserRole.USER);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void findByEmail_withUnknownEmail_returnsEmpty() {
        var result = userEntityRepository.findByEmail("nonexistent@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void save_generatesUuidOnPersist() {
        var user = UserEntity.builder()
                .username("uuidtest")
                .email("uuid@example.com")
                .password("pass")
                .role(UserRole.ADMIN)
                .createdAt(Instant.now())
                .build();

        UserEntity saved = userEntityRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId().toString()).matches(
                "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }
}
