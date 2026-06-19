# Tutorial 3: Configuracion de seguridad

> **Tiempo estimado:** 25 minutos
> **Prerequisito:** Tutoriales 1 y 2 completados
> **Archivos clave:** `SecurityConfig.java`, `ApiPaths.java`

---

## 1. El archivo mas importante

`SecurityConfig.java` es donde se ensambla toda la seguridad. Es el unico archivo que, si se cambia incorrectamente, puede romper toda la proteccion de la aplicacion.

```java
// SecurityConfig.java:22-26
@Configuration
@EnableWebSecurity    // Activa la cadena de filtros de seguridad
@EnableMethodSecurity // Permite usar @PreAuthorize en metodos
@RequiredArgsConstructor
public class SecurityConfig {
```

Las dos primeras anotaciones son criticas:
- `@EnableWebSecurity`: Activa el motor de seguridad de Spring
- `@EnableMethodSecurity`: Permite usar `@PreAuthorize` en metodos del service

---

## 2. SecurityFilterChain: las reglas de seguridad

El bean `securityFilterChain` define que pasa con cada request:

```java
// SecurityConfig.java:32-65
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1. CSRF desactivado
        .csrf(AbstractHttpConfigurer::disable)

        // 2. Sesiones desactivadas
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 3. Reglas de autorizacion
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers(ApiPaths.PUBLIC_AUTH_ENDPOINTS).permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated())

        // 4. Provider de autenticacion
        .authenticationProvider(authenticationProvider())

        // 5. Filtro JWT
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

---

## 3. Por que desactivamos CSRF

**CSRF** (Cross-Site Request Forgery) protege contra ataques donde un sitio web malicioso usa las cookies de sesion de un usuario para hacer peticiones a tu API.

Pero nosotros no usamos cookies. Cada request trae su token JWT en el header `Authorization`. Un atacante no puede "secuestrar" el JWT de otra persona porque no esta en una cookie del navegador.

```java
// SecurityConfig.java:35-36
// Desactivamos CSRF porque es una API REST. CSRF protege contra ataques
// que usan cookies de sesion del navegador, pero aca no usamos cookies
// — cada request lleva su token JWT.
.csrf(AbstractHttpConfigurer::disable)
```

---

## 4. Sesiones STATELESS

El servidor no guarda **ninguna** informacion sobre el usuario entre requests. Cada request es independiente:

```java
// SecurityConfig.java:39-41
// No creamos sesiones HTTP. El servidor no guarda estado del usuario
// entre requests. Cada peticion debe incluir su propio token JWT
// en el header Authorization.
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

Esto permite **escalar horizontalmente**: si tienes 5 servidores, cualquiera puede atender cualquier request porque no hay sesiones que sincronizar.

---

## 5. Endpoints publicos vs protegidos

Las reglas se evaluan en orden. La primera que coincida gana:

```java
// SecurityConfig.java:44-56
.authorizeHttpRequests(auth -> auth
    // 1. Swagger: publico para documentacion
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

    // 2. Auth endpoints: publicos (no requieren token previo)
    .requestMatchers(ApiPaths.PUBLIC_AUTH_ENDPOINTS).permitAll()

    // 3. Actuator: publico para monitoreo
    .requestMatchers("/actuator/**").permitAll()

    // 4. Todo lo demas: REQUIERE token JWT valido
    .anyRequest().authenticated())
```

Los endpoints publicos estan definidos en `ApiPaths.java`:

```java
// ApiPaths.java:18-22
public static final String[] PUBLIC_AUTH_ENDPOINTS = {
    "/api/auth/register",   // Crear cuenta
    "/api/auth/login",      // Obtener token
    "/api/auth/validate"    // Verificar token
};
```

**Nota importante:** `/api/auth/users` NO esta en la lista de endpoints publicos. Por eso requiere token JWT.

---

## 6. El AuthenticationProvider

El `DaoAuthenticationProvider` conecta dos componentes:

```java
// SecurityConfig.java:67-74
@Bean
public AuthenticationProvider authenticationProvider() {
    // DaoAuthenticationProvider conecta:
    // a) UserDetailsService (busca usuario en BD)
    // b) PasswordEncoder (verifica la contrasena con BCrypt)
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
}
```

Cuando alguien hace login, este provider:
1. Llama a `CustomUserDetailsService.loadUserByUsername(email)` para buscar el usuario
2. Usa `BCryptPasswordEncoder.matches()` para verificar la contrasena
3. Si ambas cosas son correctas, devuelve un objeto `Authentication`

---

## 7. El PasswordEncoder

```java
// SecurityConfig.java:81-87
@Bean
public PasswordEncoder passwordEncoder() {
    // BCrypt es un algoritmo de hashing lento que incluye una "sal" aleatoria.
    // Es el estandar para guardar contrasenas porque hace muy costoso para un
    // atacante probar contrasenas por fuerza bruta aunque tenga acceso a la BD.
    return new BCryptPasswordEncoder();
}
```

---

## Diagrama de referencia

Ver **Arquitectura General** en `docs/ARQUITECTURA.md` (diagrama 1), especialmente la seccion de Security Config.

---

## Ejercicio

1. Abre `SecurityConfig.java`. Identifica las 5 secciones del `securityFilterChain` (CSRF, sesiones, auth rules, provider, filter).
2. Abre `ApiPaths.java`. Agrega un nuevo endpoint publico: `/api/auth/public-info`. Agregalo a `PUBLIC_AUTH_ENDPOINTS` y al `requestMatchers`.
3. Corre los tests: `./mvnw test`. Deberian pasar todos.
4. Elimina el endpoint de la lista y vuelve a correr los tests.

---

## Preguntas de verificacion

1. Por que desactivamos CSRF en una API REST?
2. Que significa `SessionCreationPolicy.STATELESS`?
3. En que orden se evaluan las reglas de `authorizeHttpRequests`?
4. Que componentes conecta el `DaoAuthenticationProvider`?
5. Que pasaria si eliminas `.anyRequest().authenticated()`?

---

## Siguiente

Siguiente tutorial: [04 - Filtro JWT](04-filtro-jwt.md)
