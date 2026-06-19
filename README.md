<div align="center">

```
   █████╗ ██╗   ██╗████████╗██╗  ██╗
  ██╔══██╗██║   ██║╚══██╔══╝██║  ██║
  ███████║██║   ██║   ██║   ███████║
  ██╔══██║██║   ██║   ██║   ██╔══██║
  ██║  ██║╚██████╔╝   ██║   ██║  ██║
  ╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝

  ██████╗  ██████╗ ██████╗ ██████╗ ███████╗    ███████╗██╗   ██╗███████╗████████╗███████╗███╗   ███╗
  ██╔═══██╗██╔════╝██╔═══██╗██╔══██╗██╔════╝    ██╔════╝╚██╗ ██╔╝██╔════╝╚══██╔══╝██╔════╝████╗ ████║
  ██║   ██║██║     ██║   ██║██████╔╝█████╗      ███████╗ ╚████╔╝ ███████╗   ██║   █████╗  ██╔████╔██║
  ██║▄▄ ██║██║     ██║   ██║██╔══██╗██╔══╝      ╚════██║  ╚██╔╝  ╚════██║   ██║   ██╔══╝  ██║╚██╔╝██║
  ╚██████╔╝╚██████╗╚██████╔╝██║  ██║███████╗    ███████║   ██║   ███████║   ██║   ███████╗██║ ╚═╝ ██║
   ╚══▀▀═╝  ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝    ╚══════╝   ╚═╝   ╚══════╝   ╚═╝   ╚══════╝╚═╝     ╚═╝

```

**AUTH SERVICE — SECURITY CORE**

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Build](https://img.shields.io/badge/build-passing-brightgreen?style=for-the-badge)
![Tests](https://img.shields.io/badge/tests-47-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-blue?style=for-the-badge)

Sistema de autenticacion y autorizacion basado en Spring Security y JWT.
Implementa un flujo stateless para arquitecturas de microservicios.

[Quick Start](#quick-start)  | [Arquitectura](#arquitectura) | [Tutoriales](#aprende-spring-security)

</div>

---

## Features

- **JWT Token** — Token stateless con HMAC-SHA256 y expiracion configurable
- **BCrypt** — Password hashing con salt aleatorio (hashing intencionalmente lento)
- **RBAC** — Control de acceso por roles (USER / ADMIN) con `@PreAuthorize`
- **46 Tests** — Cobertura unit, integration y slice
- **Stateless** — Sin sesiones HTTP, ideal para microservicios
- **API Docs** — Swagger/OpenAPI habilitado en perfil dev

---

## Tech Stack

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.7-6DB33F?style=flat&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-6DB33F?style=flat&logo=springsecurity)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-3.x-6DB33F?style=flat)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql)
![JWT](https://img.shields.io/badge/JWT-jjwt_0.12.6-000000?style=flat&logo=jsonwebtokens)
![MapStruct](https://img.shields.io/badge/MapStruct-1.6.3-E63946?style=flat)
![Lombok](https://img.shields.io/badge/Lombok-1.x-6DB33F?style=flat)
![Swagger](https://img.shields.io/badge/Swagger-OpenAPI_3.0-85EA2D?style=flat&logo=swagger)
![JUnit5](https://img.shields.io/badge/JUnit_5-5.x-23913C?style=flat&logo=junit5)
![Mockito](https://img.shields.io/badge/Mockito-5.x-E91E63?style=flat)
![H2](https://img.shields.io/badge/H2_DB-Test-22c55e?style=flat)

---

## Endpoints

| Metodo | Endpoint | Auth | Descripcion |
|--------|----------|------|-------------|
| <img src="https://img.shields.io/badge/POST-22c55e?style=flat-square" /> | `/api/auth/register` | Publica | Crear usuario nuevo |
| <img src="https://img.shields.io/badge/POST-22c55e?style=flat-square" /> | `/api/auth/login` | Publica | Obtener token JWT |
| <img src="https://img.shields.io/badge/POST-22c55e?style=flat-square" /> | `/api/auth/validate` | Publica | Verificar si un token es valido |
| <img src="https://img.shields.io/badge/GET-3b82f6?style=flat-square" /> | `/api/auth/users` | <img src="https://img.shields.io/badge/ADMIN-ef4444?style=flat-square" /> | Listar usuarios |

> La documentacion interactiva esta disponible en `/swagger-ui.html` tras iniciar el proyecto.

<details>
<summary>Flujo de autenticacion</summary>

```
1. POST /register  → Crear cuenta (sin token)
2. POST /login     → Obtener token JWT
3. POST /validate  → Verificar token (header: Authorization: Bearer <token>)
4. GET /users      → Acceder con token de usuario ADMIN
```

</details>

---

## Arquitectura

| Capa | Componente | Descripcion |
|:-----|:-----------|:------------|
| `Cliente` | ![Postman](https://img.shields.io/badge/Postman-FF6C37?style=flat-square&logo=postman&logoColor=white) | Realiza requests HTTP con JWT en header |
| `JWT Filter` | ![Filter](https://img.shields.io/badge/OncePerRequestFilter-e65100?style=flat-square) | Valida token antes de llegar al controller |
| `Controller` | ![Thin](https://img.shields.io/badge/Thin_Controller-2e7d32?style=flat-square) | Recibe request, delega al service |
| `Service` | ![Auth](https://img.shields.io/badge/AuthServiceImpl-6a1b9a?style=flat-square) | Logica de negocio: login, register, RBAC |
| `Repository` | ![JPA](https://img.shields.io/badge/UserEntityRepository-f57f17?style=flat-square) | Acceso a datos via Spring Data JPA |
| `DB` | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white) | Almacena usuarios, roles, permisos |

> Diagramas detallados en [docs/ARQUITECTURA.md](docs/ARQUITECTURA.md)

---

## Estructura del Proyecto

```
src/main/java/dev/qcore/auth/
  │
  ├── config/
  │     ├── SecurityConfig.java            ← Configuracion de seguridad
  │     ├── JwtAuthFilter.java             ← Filtro JWT (8 pasos)
  │     ├── CustomUserDetailsService.java   ← Puente BD → Spring Security
  │     └── OpenApiConfig.java             ← Swagger
  │
  ├── controller/
  │     ├── AuthApi.java                   ← Interface + anotaciones Swagger
  │     └── impl/
  │           └── AuthApiController.java   ← REST controller (thin)
  │
  ├── service/
  │     ├── JwtService.java                ← Interface JWT
  │     ├── AuthService.java               ← Interface Auth
  │     └── impl/
  │           ├── JwtServiceImpl.java      ← HMAC-SHA JWT
  │           └── AuthServiceImpl.java     ← Login, registro, RBAC
  │
  ├── repository/
  │     └── UserEntityRepository.java      ← Spring Data JPA
  │
  └── common/
        ├── constants/                     ← ApiPaths, JwtConstants, ErrorCodes
        ├── enums/
        │     └── UserRole.java            ← USER, ADMIN
        ├── model/
        │     ├── entities/
        │     │     └── UserEntity.java    ← JPA + UserDetails
        │     ├── dto/
        │     │     ├── request/           ← LoginRequest, RegisterRequest
        │     │     └── response/          ← TokenResponse, ErrorResponse, etc.
        │     └── mapper/
        │           └── UserMapper.java    ← MapStruct
        └── exception/
              ├── GlobalExceptionHandler.java
              ├── DuplicateEmailException.java
              ├── TokenInvalidException.java
              ├── InvalidCredentialsException.java
              └── AccessDeniedException.java
```

---

## Quick Start

```bash
# 1. Clonar el repositorio
git clone https://github.com/user/auth.git
cd auth

# 2. Configurar variables de entorno
export JWT_SECRET="tu-secreto-aqui-minimo-32-caracteres"
export DB_HOST=localhost
export DB_NAME=auth_db
export DB_USERNAME=postgres
export DB_PASSWORD=tu-password

# 3. Ejecutar
./mvnw spring-boot:run

# 4. Abrir Swagger
open http://localhost:8080/swagger-ui.html
```

<details>
<summary>Requisitos previos</summary>

- Java 21+
- Maven 3.9+ (o usar el wrapper `./mvnw`)
- PostgreSQL 16+ corriendo

</details>

<details>
<summary>Que pasa despues de levantar la app</summary>

La app arranca en el puerto `8080` por defecto.

**Endpoints disponibles:**

| Endpoint | Metodo | Descripcion | Autenticacion |
|:---------|:-------|:------------|:--------------|
| `/api/v1/auth/login` | POST | Login con email/password | Publico |
| `/api/v1/auth/register` | POST | Registro de usuario | Publico |
| `/api/v1/auth/whoami` | GET | Informacion del usuario actual | JWT |
| `/api/v1/admin/dashboard` | GET | Panel de administracion | ADMIN |

**Swagger UI:** `http://localhost:8080/swagger-ui.html`
**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

</details>

<details>
<summary>Ejemplos curl</summary>

**1. Registrar usuario:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Perez",
    "email": "juan@example.com",
    "password": "password123"
  }'
```

Respuesta:
```json
{
  "id": "uuid-aqui",
  "name": "Juan Perez",
  "email": "juan@example.com",
  "roles": ["USER"]
}
```

**2. Login:**

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "password123"
  }'
```

Respuesta:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**3. Usar el token (whoami):**

```bash
curl -X GET http://localhost:8080/api/v1/auth/whoami \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**4. Acceder a endpoint ADMIN (requiere rol ADMIN):**

```bash
curl -X GET http://localhost:8080/api/v1/admin/dashboard \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

> Si el usuario no tiene rol `ADMIN`, recibes 403 Forbidden.

</details>

<details>
<summary>Troubleshooting</summary>

**Error: Puerto 8080 ocupado**
```
Web server failed to start. Port 8080 was already in use.
```
Solucion: Cambiar el puerto en `application.yaml` o encontrar el proceso que lo usa:
```bash
lsof -i :8080
kill -9 <PID>
```

**Error: No se puede conectar a la base de datos**
```
Connection to localhost:5432 refused
```
Solucion: Verificar que PostgreSQL esta corriendo:
```bash
# Linux/Mac
sudo systemctl status postgresql

# Docker
docker ps | grep postgres
```

**Error: JWT invalido o expirado**
```
Token invalid or expired
```
Solucion: Generar un nuevo token haciendo login nuevamente. Los tokens duran 1 hora por defecto.

**Error: 403 Forbidden**
```
Access Denied
```
Solucion: El usuario no tiene el rol necesario. Verificar los roles en la BD o registrar un nuevo usuario con rol `ADMIN`.

</details>

---

## Tests

![Tests](https://img.shields.io/badge/Tests-47%20passing-brightgreen)

```bash
./mvnw test
```

| Tipo | Tests | Archivos | Descripcion |
|------|-------|----------|-------------|
| <img src="https://img.shields.io/badge/Unit-41-22c55e?style=flat-square" /> | 41 | 7 clases | Servicios, filtros, exceptions, controller |
| <img src="https://img.shields.io/badge/Integration-3-f59e0b?style=flat-square" /> | 3 | 2 clases | Contexto Spring, seguridad con @WithMockUser |
| <img src="https://img.shields.io/badge/Slice-3-3b82f6?style=flat-square" /> | 3 | 1 clase | Repositorio JPA con H2 |

<details>
<summary>Archivos de test</summary>

| Archivo | Tests | Tipo |
|---------|-------|------|
| `JwtServiceImplTest.java` | 11 | Unit |
| `JwtAuthFilterTest.java` | 8 | Unit |
| `AuthServiceImplTest.java` | 6 | Unit |
| `GlobalExceptionHandlerTest.java` | 6 | Unit |
| `AuthApiControllerTest.java` | 5 | Unit |
| `CustomUserDetailsServiceTest.java` | 2 | Unit |
| `UserEntityTest.java` | 2 | Unit |
| `AuthServiceMethodSecurityTest.java` | 2 | Integration |
| `AuthApplicationTests.java` | 1 | Integration |
| `UserEntityRepositoryTest.java` | 3 | Slice |

</details>

---

## Aprende Spring Security

El proyecto incluye tutoriales para associates que estan aprendiendo Spring Security:

<table>
<tr>
<td>

### 1. Que es Spring Security
 Filter chain, JWT vs sesiones
<br/>
<a href="docs/01-que-es-spring-security.md">Leer tutorial →</a>

</td>
<td>

### 2. Modelo de datos
UserEntity, BCrypt, DTOs
<br/>
<a href="docs/02-modelo-de-datos.md">Leer tutorial →</a>

</td>
<td>

### 3. Configuracion
SecurityConfig, endpoints publicos
<br/>
<a href="docs/03-configuracion.md">Leer tutorial →</a>

</td>
</tr>
<tr>
<td>

### 4. Filtro JWT
Los 8 pasos del filtro
<br/>
<a href="docs/04-filtro-jwt.md">Leer tutorial →</a>

</td>
<td>

### 5. Autenticacion
Login, registro, errores
<br/>
<a href="docs/05-autenticacion.md">Leer tutorial →</a>

</td>
<td>

### 6. Roles y autorizacion
@PreAuthorize, 401 vs 403
<br/>
<a href="docs/06-roles-y-autorizacion.md">Leer tutorial →</a>

</td>
</tr>
</table>

---

## Siguientes Pasos

Lo que acabas de ver son las **bases**. Aqui tienes los proximos niveles.

### Nivel 2: Administracion
- **AdminInitializer**: crear admin al iniciar si no existe (`CommandLineRunner`)
- **Regla**: solo admins pueden hacer CRUD a otros USER
- **User Mgmt**: endpoint para listar/eliminar usuarios

### Nivel 3: Sesiones
- **Logout**: endpoint `POST /logout` + blacklist de tokens
- **Blacklist**: tabla `token_blacklist` + filtro la consulta en `JwtAuthFilter`
- **Refresh Tokens**: access + refresh token pair, endpoint `/refresh`

### Nivel 4: Produccion
- **Password Reset**: forgot-password → email con token temporal
- **Email Verify**: verificacion al registrarse (codigo o link)
- **Rate Limiting**: prevenir abuso de endpoints

---

## Seguridad

<details>
<summary>Variables de entorno requeridas</summary>

| Variable | Perfil | Descripcion | Ejemplo |
|----------|--------|-------------|---------|
| `JWT_SECRET` | Todos | Clave HMAC-SHA (min 32 chars) | `mi-secreto-super-seguro-aqui-32ch` |
| `JWT_ACCESS_EXPIRATION` | Todos | Tiempo de vida del token (ms) | `3600000` (1 hora) |
| `DB_HOST` | Todos | Host de PostgreSQL | `localhost` |
| `DB_PORT` | Todos | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Todos | Nombre de la BD | `auth_db` |
| `DB_USERNAME` | Todos | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Todos | Password de PostgreSQL | `mi-password` |
| `ACTIVE_PROFILE` | Todos | Perfil activo | `dev` |

**NUNCA** commitear secrets al repositorio.

</details>

<details>
<summary>Perfiles de configuracion</summary>

| Perfil | Swagger | JWT Secret | Base de datos |
|--------|---------|------------|---------------|
| `dev` | Habilitado | Hardcoded (inseguro) | PostgreSQL local |
| `stg` | Deshabilitado | Env var obligatoria | PostgreSQL remoto |
| `prod` | Deshabilitado | Env var obligatoria | PostgreSQL remoto |

</details>

---

<div align="center">

Hecho con Spring Security + JWT

[Documentacion](docs/ARQUITECTURA.md) | [Tutoriales](#aprende-spring-security)

</div>
