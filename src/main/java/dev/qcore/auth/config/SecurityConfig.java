package dev.qcore.auth.config;


import dev.qcore.auth.common.constants.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Activa la cadena de filtros de seguridad de Spring Security
@EnableMethodSecurity // Permite usar @PreAuthorize en métodos para controlar acceso por roles
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desactivamos CSRF porque es una API REST. CSRF protege contra ataques que usan cookies
                // de sesión del navegador, pero acá no usamos cookies — cada request lleva su token JWT.
                .csrf(AbstractHttpConfigurer::disable)

                // No creamos sesiones HTTP. El servidor no guarda estado del usuario entre requests.
                // Cada petición debe incluir su propio token JWT en el header Authorization.
                // Esto permite escalar horizontalmente sin compartir sesiones entre servidores.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Swagger UI es público para que cualquiera pueda ver la documentación
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // /register, /login y /validate son públicos porque no requieren un token previo
                        .requestMatchers(ApiPaths.PUBLIC_AUTH_ENDPOINTS).permitAll()
                        // /actuator expone métricas y salud del servicio
                        .requestMatchers("/actuator/**").permitAll()
                        // Cualquier otra ruta necesita autenticación (token JWT válido)
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // Agregamos nuestro filtro JWT ANTES del filtro de login por formulario.
                // Así, cada request pasa por JwtAuthFilter, que extrae el token del header,
                // lo valida y coloca la autenticación en el contexto de seguridad.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider conecta UserDetailsService (busca usuario en BD)
        // con PasswordEncoder (verifica la contraseña con BCrypt)
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt es un algoritmo de hashing lento que incluye una "sal" (salt) aleatoria.
        // Es el estándar para guardar contraseñas porque hace muy costoso para un atacante
        // probar contraseñas por fuerza bruta aunque tenga acceso a la base de datos.
        return new BCryptPasswordEncoder();
    }
    }