# Tutorial 4: El filtro JWT

> **Tiempo estimado:** 30 minutos
> **Prerequisito:** Tutoriales 1-3 completados
> **Archivos clave:** `JwtAuthFilter.java`, `JwtServiceImpl.java`

---

## 1. Que es OncePerRequestFilter

`JwtAuthFilter` extiende `OncePerRequestFilter`, que garantiza que el filtro se ejecute **una sola vez** por cada request HTTP. Sin esta clase, un filtro podria ejecutarse multiples veces si hay forward/redirect internos.

```java
// JwtAuthFilter.java:27-29
// OncePerRequestFilter garantiza que este filtro se ejecute UNA SOLA VEZ
// por cada peticion HTTP. Su trabajo: interceptar cada request, extraer
// el token JWT del header Authorization, validarlo y, si es correcto,
// autenticar al usuario en el sistema.
```

---

## 2. Los 8 pasos del filtro

El filtro tiene 8 pasos numerados. Seguiremos cada uno:

```java
// JwtAuthFilter.java:44-97
```

### Paso 1: Extraer el header Authorization

```java
// JwtAuthFilter.java:44
// 1. Extraer el header "Authorization" de la peticion HTTP
final String authHeader = request.getHeader(HeaderConstants.AUTHORIZATION);
```

Cada request HTTP puede traer un header llamado `Authorization`. Si no existe, `authHeader` sera `null`.

### Paso 2: Verificar si hay header Bearer

```java
// JwtAuthFilter.java:47-48
// 2. Si no hay header o no empieza con "Bearer ", continuamos la cadena
//    sin autenticar. Esto permite que rutas publicas funcionen sin token.
if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
    filterChain.doFilter(request, response);
    return;
}
```

El formato del header es: `Authorization: Bearer eyJhbGciOiJIUzI1...`

Si el header no existe, o no empieza con `"Bearer "`, el filtro **no hace nada** y deja que la request continúe. Esto es como decir: "no tengo passaporte, pero dejo que la request pase por si es una ruta publica".

### Paso 3: Extraer el token

```java
// JwtAuthFilter.java:54
// 3. Extraer solo el token JWT (quitamos el prefijo "Bearer ")
final String jwt = authHeader.substring(JwtConstants.BEARER_PREFIX.length());
```

`JwtConstants.BEARER_PREFIX` es `"Bearer "` (con espacio). Al hacer `substring`, nos quedamos solo con el token: `eyJhbGciOiJIUzI1NiJ9...`

### Paso 4: Extraer el email del token

```java
// JwtAuthFilter.java:58-59
// 4. Intentar extraer el email del token. Si el token esta mal formado,
//    tiene firma invalida o expiro, capturamos la excepcion y seguimos sin autenticar.
try {
    userEmail = jwtService.extractEmail(jwt);
} catch (Exception e) {
    log.warn("Token JWT invalido o expirado: {}", e.getMessage());
    filterChain.doFilter(request, response);
    return;
}
```

`jwtService.extractEmail()` parsea el JWT y extrae el claim `sub` (subject), que es el email. Si el token esta corrupto, lanza una excepcion que capturamos y seguimos sin autenticar.

### Paso 5: Verificar que no haya autenticacion previa

```java
// JwtAuthFilter.java:68-69
// 5. Si tenemos un email valido y no hay una autenticacion previa en el contexto...
if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
```

Esta es una proteccion: si ya hay alguien autenticado en este request (por ejemplo, otro filtro ya lo hizo), no lo sobreescribimos.

### Paso 6: Cargar el usuario y verificar expiracion

```java
// JwtAuthFilter.java:70-74
// Cargamos el usuario completo desde la base de datos (incluyendo sus roles)
UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

// 6. Verificamos que el token no haya expirado
if (!jwtService.isExpired(jwt)) {
```

Dos cosas pasan aqui:
1. Se busca el usuario en la BD por email (para obtener sus roles actualizados)
2. Se verifica que el token no este expirado

### Paso 7: Crear la autenticacion

```java
// JwtAuthFilter.java:75-82
// Creamos el token de autenticacion de Spring Security con los datos del usuario.
// El segundo parametro (credenciales) va en null porque el JWT ya fue validado.
// Los authorities (roles) permiten que @PreAuthorize funcione despues.
UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
    userDetails,    // principal: quien es el usuario
    null,           // credentials: null porque el JWT ya se valido
    userDetails.getAuthorities()  // roles: ROLE_USER o ROLE_ADMIN
);

// Agregamos detalles de la peticion (IP, sesion, etc.)
authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
```

**Punto clave:** El segundo parametro es `null` porque no necesitamos la contrasena. El JWT ya se valido en el paso anterior. Las `authorities` son los roles del usuario, que permiten que `@PreAuthorize` funcione despues.

### Paso 8: Guardar en el SecurityContext

```java
// JwtAuthFilter.java:87-90
// 7. Guardamos la autenticacion en el SecurityContextHolder.
//    A partir de este momento, Spring Security "sabe" quien es el usuario
//    para esta peticion. Se puede acceder con @AuthenticationPrincipal
//    o SecurityContextHolder.getContext().getAuthentication().
SecurityContextHolder.getContext().setAuthentication(authToken);
```

`SecurityContextHolder` es un **ThreadLocal** que guarda la autenticacion actual. A partir de esta linea, cualquier codigo en el mismo hilo puede acceder al usuario con:

```java
SecurityContextHolder.getContext().getAuthentication()
// o
@AuthenticationPrincipal UserDetails userDetails
```

### Siempre continuamos la cadena

```java
// JwtAuthFilter.java:96-97
// 8. Siempre continuamos la cadena de filtros para que la peticion llegue al controlador
filterChain.doFilter(request, response);
```

**Importante:** El filtro siempre llama a `doFilter`, sin importar si autentico o no. La diferencia es:
- Si autentico: el controlador recibe un request con usuario autenticado
- Si no autentico: el controlador recibe un request sin autenticacion (y puede fallar si es una ruta protegida)

---

## 3. El SecurityContextHolder

`SecurityContextHolder` es como una "memoria temporal" del hilo de ejecucion:

```
Hilo HTTP #1:
  SecurityContext = { usuario: "ana@email.com", roles: [ROLE_ADMIN] }
  Controller: "Hola Ana, eres admin"

Hilo HTTP #2:
  SecurityContext = { usuario: "luis@email.com", roles: [ROLE_USER] }
  Controller: "Hola Luis, eres user"
```

Cada request se ejecuta en un hilo diferente, y cada hilo tiene su propio SecurityContext. Por eso el servidor puede atender miles de usuarios simultaneamente sin mezclar sus identidades.

---

## 4. Flujo completo del filtro

```
Request HTTP
    │
    ▼
Paso 1: Extraer header "Authorization"
    │
    ▼
Paso 2: ¿Tiene "Bearer "?
    │ NO → doFilter() (pasa sin autenticar)
    │ SI
    ▼
Paso 3: Extraer token (quitar "Bearer ")
    │
    ▼
Paso 4: extractEmail(token)
    │ Excepción → log.warn → doFilter()
    │ email valido
    ▼
Paso 5: ¿Ya hay autenticacion en contexto?
    │ SI → doFilter()
    │ NO
    ▼
Paso 6: loadUserByUsername(email) + isExpired(token)
    │ expirado → doFilter()
    │ valido
    ▼
Paso 7: Crear UsernamePasswordAuthenticationToken
    │
    ▼
Paso 8: SecurityContextHolder.setAuthentication(authToken)
    │
    ▼
doFilter() → Request autenticada llega al controller
```

---

## Diagrama de referencia

Ver **Filtro JWT - Arbol de Decision** en `docs/ARQUITECTURA.md` (diagrama 4).

---

## Ejercicio

1. Abre `JwtAuthFilter.java`. Sigue los 8 pasos y encadralos con el codigo real.
2. Abre `JwtAuthFilterTest.java`. Que escenarios se prueban? Identifica cual test cubre cada paso.
3. Agrega un `log.debug("Paso 4: email extraido = {}", userEmail)` despues de la linea 61.
4. Corre los tests: `./mvnw test`. Deberian seguir pasando.

---

## Preguntas de verificacion

1. Por que el filtro siempre llama a `doFilter()` al final?
2. Que pasa si el header Authorization no existe?
3. Por que el segundo parametro de `UsernamePasswordAuthenticationToken` es `null`?
4. Que es `SecurityContextHolder` y como se accede a la autenticacion?
5. Por que verificamos si ya hay una autenticacion en el contexto (paso 5)?

---

## Siguiente

Siguiente tutorial: [05 - Autenticacion y registro](05-autenticacion.md)
