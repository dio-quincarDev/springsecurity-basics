# Tutorial 5: Autenticacion y registro

> **Tiempo estimado:** 25 minutos
> **Prerequisito:** Tutoriales 1-4 completados
> **Archivos clave:** `AuthServiceImpl.java`, `AuthApiController.java`

---

## 1. El controller es "thin"

El controller **no tiene logica de negocio**. Solo recibe la request, delega en el service, y devuelve la respuesta:

```java
// AuthApiController.java:22-24
// Controlador REST que implementa los endpoints de AuthApi.
// NO contiene logica de negocio ni decisiones sobre codigos HTTP
// — solo recibe la peticion, delega en AuthService y devuelve la respuesta.
```

Toda la logica esta en `AuthServiceImpl.java`.

---

## 2. Flujo de Login

El login tiene 3 pasos:

```java
// AuthServiceImpl.java:77-104
public TokenResponse login(@Valid LoginRequest loginRequest) {
```

### Paso 1: Autenticar credenciales

```java
// AuthServiceImpl.java:80-84
// 1. authenticationManager.authenticate() delega en DaoAuthenticationProvider
//    (configurado en SecurityConfig), que:
//    a) Llama a CustomUserDetailsService.loadUserByUsername() para buscar el usuario
//    b) Usa BCryptPasswordEncoder.matches() para verificar la contrasena
//    Si algo falla, lanza BadCredentialsException → GlobalExceptionHandler → 401
Authentication authentication = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(
        loginRequest.email(),
        loginRequest.password()
    )
);
```

`AuthenticationManager` es el orquestador. No verifica nada directamente, sino que delega en el `DaoAuthenticationProvider` que configuramos en `SecurityConfig`.

### Paso 2: Obtener el usuario

```java
// AuthServiceImpl.java:92-93
// 2. Si llegamos aqui, la autenticacion fue exitosa.
//    Obtenemos el usuario autenticado del objeto Authentication.
UserEntity user = (UserEntity) authentication.getPrincipal();
```

El `principal` es el objeto que `CustomUserDetailsService` devolvio (un `UserEntity`). Lo casteamos para acceder a sus campos.

### Paso 3: Generar el JWT

```java
// AuthServiceImpl.java:96-97
// 3. Generamos un JWT con los datos del usuario.
//    El cliente usara este token en adelante en el header Authorization.
log.info("Login exitoso para usuario: {}", user.getEmail());
return jwtService.generateToken(
    user.getEmail(),
    user.getId().toString(),
    user.getUsername(),
    user.getRole().name());
```

El token contiene: email (subject), userId, username, role. El cliente lo usara en todas las requests futuras.

---

## 3. Flujo de Registro

El registro tiene 5 pasos:

```java
// AuthServiceImpl.java:44-74
public RegisterResponse createUser(@Valid RegisterRequest registerRequest) {
```

### Paso 1: Verificar email duplicado

```java
// AuthServiceImpl.java:47-48
// 1. Verificar que el email no este registrado. Si ya existe, lanzamos excepcion
//    que el GlobalExceptionHandler convierte en HTTP 409 Conflict.
if (userEntityRepository.findByEmail(registerRequest.email()).isPresent()) {
    throw new DuplicateEmailException(ErrorCodes.USER_ALREADY_EXISTS);
}
```

### Paso 2: Convertir DTO a entidad

```java
// AuthServiceImpl.java:54
// 2. Convertir el DTO de request a entidad (UserMapper asigna rol USER por defecto)
UserEntity userToSave = userMapper.toUserEntity(registerRequest);
```

### Paso 3: Hashear la contrasena

```java
// AuthServiceImpl.java:57-59
// 3. NUNCA guardamos la contrasena en texto plano.
//    BCrypt la hashea con una "sal" aleatoria antes de persistirla.
//    Asi, aunque alguien acceda a la BD, no puede obtener la contrasena original.
userToSave.setPassword(passwordEncoder.encode(registerRequest.password()));
```

### Paso 4: Guardar en BD

```java
// AuthServiceImpl.java:63-64
// 4. Guardar en base de datos
UserEntity userCreated = userEntityRepository.save(userToSave);
```

### Paso 5: Devolver respuesta

```java
// AuthServiceImpl.java:67-68
// 5. Devolver los datos del usuario creado (sin la contrasena, obviamente)
return new RegisterResponse(
    userCreated.getId().toString(),
    userCreated.getUsername(),
    userCreated.getEmail(),
    userCreated.getRole().name(),
    userCreated.getCreatedAt());
```

**Nunca** devolver la contrasena en la respuesta.

---

## 4. Validacion de token

El endpoint de validacion es publico pero requiere el header `Authorization`:

```java
// AuthServiceImpl.java:106-117
public TokenPayload validateToken(String authHeader) {
    // Extrae el token del header "Authorization" quitando el prefijo "Bearer "
    String token = authHeader.replace(JwtConstants.BEARER_PREFIX, "");
    // Delega en JwtService que verifica la firma y la expiracion del token.
    TokenPayload payload = jwtService.validateToken(token);
    // Si el token no es valido, lanza excepcion que el GlobalExceptionHandler convierte en 401
    if (!payload.valid()) {
        throw new TokenInvalidException(payload.error(), payload.message());
    }
    return payload;
}
```

---

## 5. El GlobalExceptionHandler

Cuando algo falla, las excepciones se convierten en respuestas HTTP automaticamente:

| Excepcion | HTTP | Cuando ocurre |
|-----------|------|---------------|
| `BadCredentialsException` | 401 | Email o contrasena incorrectos |
| `DuplicateEmailException` | 409 | Email ya registrado |
| `TokenInvalidException` | 401 | Token expirado o invalido |
| `AccessDeniedException` | 403 | Sin permiso para el recurso |
| `MethodArgumentNotValidException` | 400 | Datos de entrada invalidos |

```java
// GlobalExceptionHandler.java:21-25
@ExceptionHandler(InvalidCredentialsException.class)
public ResponseEntity<ErrorResponse> handleInvalidCredentials(...) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
        ErrorResponse.builder()
            .error(ErrorCodes.INVALID_CREDENTIALS)
            .message(error.getMessage())
            .timestamp(Instant.now()).build());
}
```

---

## Diagrama de referencia

Ver **Flujo de Login** y **Flujo de Registro** en `docs/ARQUITECTURA.md` (diagramas 2 y 3).

---

## Ejercicio

1. Abre `AuthServiceImpl.java`. Sigue el flujo de login (lineas 77-104) paso a paso.
2. Abre `AuthServiceImplTest.java`. Que escenarios se prueban? Identifica que mock devuelve que en cada caso.
3. Abre `GlobalExceptionHandlerTest.java`. Verifica que cada excepcion se mapea al HTTP correcto.
4. Pregunta: Que pasaria si eliminas la validacion `@Valid` del parametro de `login()`?

---

## Preguntas de verificacion

1. Que hace `authenticationManager.authenticate()` y que componente lo resuelve?
2. Por que el controller es "thin" y toda la logica esta en el service?
3. Que pasa cuando el email ya existe en el registro?
4. Como se mapean las excepciones a codigos HTTP?
5. Por que `validateToken` es un endpoint publico pero necesita el header Authorization?

---

## Siguiente

Siguiente tutorial: [06 - Roles y autorizacion](06-roles-y-autorizacion.md)
