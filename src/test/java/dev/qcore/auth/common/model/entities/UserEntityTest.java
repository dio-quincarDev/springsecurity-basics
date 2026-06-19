package dev.qcore.auth.common.model.entities;

import dev.qcore.auth.common.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    @Test
    void getAuthorities_withUserRole_returnsRoleUser() {
        var entity = UserEntity.builder().role(UserRole.USER).build();

        var authorities = entity.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void getAuthorities_withAdminRole_returnsRoleAdmin() {
        var entity = UserEntity.builder().role(UserRole.ADMIN).build();

        var authorities = entity.getAuthorities();

        assertThat(authorities).hasSize(1);
        assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }
}
