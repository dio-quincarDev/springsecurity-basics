# Tutorial 2: Modelo de datos

> **Tiempo estimado:** 20 minutos
> **Prerequisito:** Tutorial 1 completado
> **Archivos clave:** `UserEntity.java`, `UserMapper.java`, DTOs en `common/model/dto/`

---

## 1. El puente entre JPA y Spring Security

Spring Security necesita un objeto que implemente la interfaz `UserDetails`. Esta interfaz le dice a Spring Security: "este es un usuario valido, tiene contrasena y tiene roles".

Nuestro `UserEntity` hace ambas cosas:

```java
// UserEntity.java:22
public class UserEntity implements UserDetails {
```

Esto significa que la **misma entidad** que se guarda en la BD tambien es la que Spring Security usa para autenticar. No necesitamos una clase separada.

---

## 2. Los roles

Los roles determinan que puede hacer cada usuario. En nuestro proyecto hay dos:

```java
// UserRole.java
public enum UserRole {
    USER,
    ADMIN
}
```

Y se mapean a las authorities de Spring Security asi:

```java
// UserEntity.java:46-48
@Override
public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
}
```

El prefijo `ROLE_` es un convenio de Spring Security. Cuando usas `@PreAuthorize("hasRole('ADMIN')")`, Spring automaticamente agrega el prefijo `ROLE_` y compara con las authorities del usuario.

```
UserRole.USER  →  GrantedAuthority("ROLE_USER")
UserRole.ADMIN →  GrantedAuthority("ROLE_ADMIN")
```

---

## 3. Por que existen los DTOs

Nunca debes exponer la entidad JPA directamente al cliente. Hay varias razones:

1. **Seguridad:** La entidad tiene el campo `password`. Si la devuelves en un response, expones el hash.
2. **Flexibilidad:** El cliente no necesita saber el UUID o el `createdAt` en todos los casos.
3. **Validacion:** Los DTOs tienen reglas de validacion (`@NotBlank`, `@Email`, `@Size`).

```
Cliente  ──Request──>  LoginRequest (DTO)  ──>  Service  ──>  UserEntity (BD)
Cliente  <──Response──  TokenResponse (DTO)  <──  Service  <──  UserEntity (BD)
```

### DTOs de request

```java
// LoginRequest.java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

// RegisterRequest.java
public record RegisterRequest(
    @NotBlank @Pattern(regexp = "^\\S+$") String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$") String password
) {}
```

La contrasena de registro requiere: minimo 8 caracteres, 1 mayuscula, 1 numero.

### DTOs de response

| DTO | Uso | Campos |
|-----|-----|--------|
| `TokenResponse` | Despues del login | token, tokenType, expiresIn, userId |
| `RegisterResponse` | Despues del registro | id, username, email, role, createdAt |
| `TokenPayload` | Validar token | valid, userId, username, role, error, message |
| `UserSummary` | Listar usuarios (admin) | id, username, email, role |
| `ErrorResponse` | Errores | error, message, timestamp |

---

## 4. BCrypt: como se guardan las contrasenas

NUNCA se guarda la contrasena en texto plano. BCrypt es un algoritmo de hashing que:

1. Genera una **sal** (salt) aleatoria unica para cada contrasena
2. Mezcla la contrasena con la sal
3. Aplica el algoritmo de hashing (que es intencionalmente lento)
4. Devuelve un hash que contiene la sal integrada

```java
// AuthServiceImpl.java:57-59
// NUNCA guardamos la contrasena en texto plano.
// BCrypt la hashea con una "sal" aleatoria antes de persistirla.
// Asi, aunque alguien acceda a la BD, no puede obtener la contrasena original.
userToSave.setPassword(passwordEncoder.encode(registerRequest.password()));
```

### Que es "lento" y por que es bueno

BCrypt esta disenado para ser lento (~100ms por hash). Esto es intencional:
- Un atacante que obtenga la BD no puede probar millones de contrasenas por segundo
- Cada hash tarda lo mismo, asi que la fuerza bruta es impracticable

### Como se verifica la contrasena

```java
// En el login, AuthenticationManager llama a:
passwordEncoder.matches(password, storedHash)
// BCrypt extrae la sal del hash, aplica el mismo algoritmo a la contrasena
// ingresada, y compara los resultados.
```

---

## 5. El mapper (UserMapper)

MapStruct genera automaticamente el codigo para convertir entre DTOs y entidades:

```java
// UserMapper.java
@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)   // La password se setea aparte (BCrypt)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", constant = "USER")    // Rol por defecto: USER
    UserEntity toUserEntity(RegisterRequest registerRequest);
}
```

El rol por defecto es `USER`. No hay forma de que un usuario se registre como `ADMIN` por su cuenta.

---

## Diagrama de referencia

Ver **Flujo de Registro** en `docs/ARQUITECTURA.md` (diagrama 3).

---

## Ejercicio

1. Abre `UserEntity.java`. Que campos tiene la entidad? Cual es el tipo de `id`?
2. Abre `RegisterRequest.java`. Que validaciones tiene el campo `password`?
3. Abre `UserMapper.java`. Que campos se ignoran al mapear?
4. Busca en los tests (`UserEntityTest.java`) como se verifican los roles.

---

## Preguntas de verificacion

1. Por que `UserEntity` implementa `UserDetails`?
2. Que prefijo se agrega a los roles y por que?
3. Que hace BCrypt con la contrasena antes de guardarla?
4. Por que los DTOs de response no incluyen el campo `password`?
5. Que pasa si un usuario intenta registrarse con un email que ya existe?

---

## Siguiente

Siguiente tutorial: [03 - Configuracion de seguridad](03-configuracion.md)
