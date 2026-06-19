package dev.qcore.auth.config;

import dev.qcore.auth.common.constants.HeaderConstants;
import dev.qcore.auth.common.constants.JwtConstants;
import dev.qcore.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
// OncePerRequestFilter garantiza que este filtro se ejecute UNA SOLA VEZ por cada petición HTTP.
// Su trabajo: interceptar cada request, extraer el token JWT del header Authorization,
// validarlo y, si es correcto, autenticar al usuario en el sistema.
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraer el header "Authorization" de la petición HTTP
        final String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);

        // 2. Si no hay header o no empieza con "Bearer ", continuamos la cadena sin autenticar.
        //    Esto permite que rutas públicas (register, login) funcionen sin token.
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer solo el token JWT (quitamos el prefijo "Bearer ")
        final String jwt = authHeader.substring(JwtConstants.BEARER_PREFIX.length());
        final String userEmail;

        // 4. Intentar extraer el email del token. Si el token está mal formado,
        //    tiene firma inválida o expiró, capturamos la excepción y seguimos sin autenticar.
        try {
            userEmail = jwtService.extractEmail(jwt);
        } catch (Exception e) {
            log.warn("Token JWT inválido o expirado: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Si tenemos un email válido y no hay una autenticación previa en el contexto...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Cargamos el usuario completo desde la base de datos (incluyendo sus roles)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 6. Verificamos que el token no haya expirado
            if (!jwtService.isExpired(jwt)) {
                // Creamos el token de autenticación de Spring Security con los datos del usuario.
                // El segundo parámetro (credenciales) va en null porque el JWT ya fue validado.
                // Los authorities (roles) permiten que @PreAuthorize funcione después.
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                // Agregamos detalles de la petición (IP, sesión, etc.)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Guardamos la autenticación en el SecurityContextHolder.
                //    A partir de este momento, Spring Security "sabe" quién es el usuario
                //    para esta petición. Se puede acceder con @AuthenticationPrincipal
                //    o SecurityContextHolder.getContext().getAuthentication().
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Usuario {} autenticado correctamente.", userDetails.getUsername());
            }
        }

        // 8. Siempre continuamos la cadena de filtros para que la petición llegue al controlador
        filterChain.doFilter(request, response);
    }
}