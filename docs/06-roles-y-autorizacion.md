# Tutorial 6: Roles y autorizacion

> **Tiempo estimado:** 20 minutos
> **Prerequisito:** Tutoriales 1-5 completados
> **Archivos clave:** `AuthServiceImpl.java:119-133`, `UserEntity.java:46-48`

---

## 1. Autorizacion vs Autenticacion

Son dos conceptos diferentes:

| | Autenticacion | Autorizacion |
|---|---|---|
| **Pregunta** | Quien eres? | Que puedes hacer? |
| **Cuando ocurre** | En el filtro JWT (paso 7-8) | En el controller/service (con `@PreAuthorize`) |
| **Componente** | `JwtAuthFilter` | `@PreAuthorize` + `@EnableMethodSecurity` |

Primero te autenticas (el filtro JWT verifica tu token), y luego se verifica si tienes permiso para la accion (`@PreAuthorize`).

---

## 2. @PreAuthorize

La anotacion `@PreAuthorize` se ejecuta **antes** del metodo. Spring Security verifica la condicion y, si falla, ni siquiera entra al metodo:

```java
// AuthServiceImpl.java:119-124
@Override
@PreAuthorize("hasRole('ADMIN')")
// @PreAuthorize (gracias a @EnableMethodSecurity en SecurityConfig) verifica
// automaticamente que el usuario autenticado tenga el rol ADMIN antes de
// ejecutar este metodo.
// Si el usuario tiene rol USER, Spring Security lanza AccessDeniedException → 403.
// Si no esta autenticado, lanza AuthenticationException → 401.
public List<UserSummary> listUsers() {
```

### Que evaluacion se hace

`hasRole('ADMIN')` compara:
1. Las `authorities` del usuario autenticado (que estan en `SecurityContextHolder`)
2. Con el rol requerido (`ROLE_ADMIN`)

Si el usuario tiene `ROLE_USER` pero no `ROLE_ADMIN`, falla.

---

## 3. Cuando falla la autorizacion

Hay dos casos de fallo:

### Caso 1: Usuario autenticado pero sin permiso (403)

```
Usuario con ROLE_USER intenta acceder a /users
    │
    ▼
@PreAuthorize("hasRole('ADMIN')") → false
    │
    ▼
Spring lanza AccessDeniedException
    │
    ▼
GlobalExceptionHandler lo convierte en 403 FORBIDDEN
    │
    ▼
{error: "ACCESS_DENIED", message: "..."}
```

### Caso 2: Usuario no autenticado (401)

```
Request sin token JWT intenta acceder a /users
    │
    ▼
JwtAuthFilter no encuentra autenticacion en SecurityContext
    │
    ▼
Controller recibe request sin autenticacion
    │
    ▼
@PreAuthorize falla porque no hay usuario
    │
    ▼
Spring lanza AuthenticationException
    │
    ▼
GlobalExceptionHandler lo convierte en 401 UNAUTHORIZED
```

---

## 4. Como funciona Internamente

El flujo completo es:

```
1. JwtAuthFilter ejecuta paso 8: setAuthentication(authToken)
   → SecurityContext ahora tiene: {user: "ana@email.com", roles: [ROLE_ADMIN]}

2. Controller recibe el request

3. @PreAuthorize("hasRole('ADMIN')") se evalua
   → Spring busca en SecurityContext las authorities del usuario
   → Encuentra [ROLE_ADMIN]
   → hasRole('ADMIN') compara: ROLE_ADMIN == ROLE_ADMIN → true
   → Metodo se ejecuta

4. Si el usuario fuera ROLE_USER:
   → hasRole('ADMIN') compara: ROLE_USER != ROLE_ADMIN → false
   → Spring lanza AccessDeniedException
   → Metodo NUNCA se ejecuta
```

---

## 5. El prefijo ROLE_

Spring Security maneja un convenio para los roles:

```java
// UserEntity.java:46-48
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
}
```

Y en el JWT, el rol se guarda sin el prefijo:

```java
// JwtServiceImpl.java:47-48
// Si el rol tiene prefijo "ROLE_" (ej: "ROLE_ADMIN"), lo normalizamos quitandolo.
// Spring Security usa el prefijo internamente, pero en el JWT guardamos el valor limpio.
String normalizedRole = role.startsWith(JwtConstants.ROLE_PREFIX)
    ? role.substring(JwtConstants.ROLE_PREFIX.length())
    : role;
```

Esto significa:
- En la BD: `USER` o `ADMIN` (enum)
- En memoria (Spring Security): `ROLE_USER` o `ROLE_ADMIN` ( GrantedAuthority)
- En el JWT: `USER` o `ADMIN` (sin prefijo)

Cuando `@PreAuthorize` evalua `hasRole('ADMIN')`, Spring automaticamente agrega el prefijo `ROLE_` y compara con las authorities del usuario.

---

## 6. @EnableMethodSecurity

Sin esta anotacion en `SecurityConfig`, `@PreAuthorize` no funciona:

```java
// SecurityConfig.java:24
@EnableMethodSecurity // Permite usar @PreAuthorize en metodos para controlar acceso por roles
```

Si la eliminas, el metodo `listUsers()` se ejecutaria para cualquier usuario autenticado, sin importar su rol.

---

## 7. Estrategia de roles en nuestro proyecto

| Endpoint | Roles permitidos | Como se protege |
|----------|-----------------|-----------------|
| `POST /register` | Publico (sin auth) | En `SecurityConfig` (`.permitAll()`) |
| `POST /login` | Publico (sin auth) | En `SecurityConfig` (`.permitAll()`) |
| `POST /validate` | Publico (sin auth) | En `SecurityConfig` (`.permitAll()`) |
| `GET /users` | Solo ADMIN | `@PreAuthorize("hasRole('ADMIN')")` |

Para agregar un nuevo endpoint protegido:
1. Agregarlo al controller
2. Agregar `@PreAuthorize("hasRole('...')")` en el service
3. Asegurarse de que `@EnableMethodSecurity` este habilitado

---

## Diagrama de referencia

Ver **Flujo de Autorizacion (Request Protegido)** en `docs/ARQUITECTURA.md` (diagrama 5).

---

## Ejercicio

1. Abre `AuthServiceMethodSecurityTest.java`. Que hace `@WithMockUser(roles = "ADMIN")`? Que hace `@WithMockUser(roles = "USER")`?
2. Abre `AuthServiceImpl.java:119-133`. Que pasaria si cambias `hasRole('ADMIN')` por `hasRole('USER')`?
3. Crea un nuevo endpoint `GET /api/auth/profile` que devuelva los datos del usuario autenticado (sin restriccion de rol). Usa `@AuthenticationPrincipal` para obtener el usuario.
4. Corre los tests: `./mvnw test`. Deberian seguir pasando.

---

## Preguntas de verificacion

1. Cual es la diferencia entre autenticacion y autorizacion?
2. Que pasa cuando `@PreAuthorize` falla y el usuario tiene rol USER?
3. Que pasa cuando `@PreAuthorize` falla y el usuario no esta autenticado?
4. Por que el rol se guarda como `USER` en la BD pero como `ROLE_USER` en Spring Security?
5. Que pasaria si eliminas `@EnableMethodSecurity` de `SecurityConfig`?

---

## Fin de los tutoriales

Felicidades, completaste los 6 tutoriales de Spring Security.

### Resumen de lo aprendido

| Tutorial | Concepto clave |
|----------|---------------|
| 1 | Filter chain, JWT vs sesiones |
| 2 | UserEntity, UserDetails, BCrypt, DTOs |
| 3 | SecurityConfig, endpoints publicos/protegidos |
| 4 | JwtAuthFilter, 8 pasos, SecurityContextHolder |
| 5 | Login, registro, AuthenticationManager |
| 6 | @PreAuthorize, roles, 401 vs 403 |

### Proximos pasos

1. Lee `docs/ARQUITECTURA.md` para ver los diagramas de los flujos
2. Explora los tests en `src/test/` para ver escenarios adicionales
3. Intenta agregar un nuevo endpoint protegido con un rol nuevo
