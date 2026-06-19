package dev.qcore.auth.config;

import dev.qcore.auth.repository.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// UserDetailsService es el puente que usa Spring Security para buscar usuarios en la base de datos.
// Cuando alguien intenta autenticarse (login), Spring Security llama automáticamente a
// loadUserByUsername() con el email recibido.
public class CustomUserDetailsService implements UserDetailsService {
    private final UserEntityRepository userEntityRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Busca al usuario en la BD por email. Si existe, lo devuelve.
        // UserEntity implementa UserDetails, así que ya trae la información de roles (getAuthorities()).
        // Si no existe, lanza UsernameNotFoundException, y Spring Security responde con 401.
        return userEntityRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));
    }
}
