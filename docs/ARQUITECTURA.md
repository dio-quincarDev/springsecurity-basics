# Arquitectura — Auth API

> Diagramas de la autenticación JWT con Spring Security.
> Cada diagrama referencia las líneas exactas del código fuente.

---

## 1. Arquitectura General

```mermaid
graph TB
    subgraph CLIENT["CLIENTE"]
        C["Postman / Frontend"]
    end

    subgraph FILTER_CHAIN["CADENA DE FILTROS DE SEGURIDAD"]
        direction LR
        CSRF["CSRF — Desactivado — API REST stateless"]
        SESSION["Sesiones — STATELESS — sin cookies"]
        JWT_FILTER["JwtAuthFilter — OncePerRequestFilter — Extrae · Valida · Autentica"]
    end

    subgraph SECURITY_CONFIG["CONFIGURACIÓN"]
        SC["SecurityFilterChain — SecurityConfig.java:33-65"]
        AUTH_PROVIDER["DaoAuthenticationProvider — UserDetailsService + BCrypt"]
        AUTH_MANAGER["AuthenticationManager — Orquesta la autenticación"]
    end

    subgraph CONTROLLER["CAPA CONTROLLER"]
        CTRL["AuthApiController — POST /register · /login · /validate · GET /users"]
    end

    subgraph SERVICE["CAPA SERVICE"]
        AUTH_SVC["AuthServiceImpl — @PreAuthorize('hasRole(ADMIN)') · BCrypt · Login · Validación"]
        JWT_SVC["JwtServiceImpl — generateToken() HMAC-SHA · validateToken() firma + expiración · extractEmail() / extractRole()"]
    end

    subgraph REPOSITORY["CAPA REPOSITORY"]
        REPO["UserEntityRepository — Spring Data JPA — findByEmail()"]
    end

    subgraph DATABASE["BASE DE DATOS"]
        DB["PostgreSQL — users (UUID, email, password, role)"]
    end

    subgraph EXCEPTIONS["MANEJO DE ERRORES"]
        GEX["GlobalExceptionHandler — 401 · 403 · 409 · 400"]
    end

    C -->|"HTTP Request"| FILTER_CHAIN
    FILTER_CHAIN --> SC
    SC --> AUTH_PROVIDER
    SC --> AUTH_MANAGER
    FILTER_CHAIN --> CTRL
    CTRL --> AUTH_SVC
    AUTH_SVC --> JWT_SVC
    AUTH_SVC --> REPO
    JWT_SVC -.->|"extrae email"| FILTER_CHAIN
    REPO --> DB
    AUTH_SVC -.->|"excepciones"| GEX
    AUTH_SVC -.->|"credenciales inválidas"| GEX

    style CLIENT fill:#1565c0,stroke:#0d47a1,color:#ffffff
    style FILTER_CHAIN fill:#e65100,stroke:#bf360c,color:#ffffff
    style SECURITY_CONFIG fill:#c62828,stroke:#b71c1c,color:#ffffff
    style CONTROLLER fill:#2e7d32,stroke:#1b5e20,color:#ffffff
    style SERVICE fill:#6a1b9a,stroke:#4a148c,color:#ffffff
    style REPOSITORY fill:#f57f17,stroke:#e65100,color:#ffffff
    style DATABASE fill:#37474f,stroke:#263238,color:#ffffff
    style EXCEPTIONS fill:#b71c1c,stroke:#880e0e,color:#ffffff
```

---

## 2. Flujo de Login

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant CTRL as AuthApiController
    participant AUTH as AuthServiceImpl
    participant AM as AuthenticationManager
    participant UDS as CustomUserDetailsService
    participant DB as PostgreSQL
    participant BC as BCrypt
    participant JWT as JwtServiceImpl

    C->>+CTRL: POST /api/auth/login {email, password}
    CTRL->>+AUTH: login(LoginRequest)
    AUTH->>+AM: authenticate(UsernamePasswordAuthenticationToken)
    AM->>+UDS: loadUserByUsername(email)
    UDS->>+DB: findByEmail(email)
    DB-->>-UDS: UserEntity
    UDS-->>-AM: UserDetails
    AM->>+BC: matches(password, storedHash)
    BC-->>-AM: true
    AM-->>-AUTH: Authentication (UserDetails)
    AUTH->>AUTH: (UserEntity) authentication.getPrincipal()
    AUTH->>+JWT: generateToken(email, userId, username, role)
    JWT->>JWT: Construir JWT con claims {sub, userId, username, role} + firma HMAC-SHA
    JWT-->>-AUTH: TokenResponse
    AUTH-->>-CTRL: TokenResponse
    CTRL-->>-C: 200 OK {token, tokenType: "Bearer", expiresIn: 3600, userId}
```

> **Flujo de error:** Si las credenciales son incorrectas, `AuthenticationManager` lanza `BadCredentialsException` que `GlobalExceptionHandler` convierte en **401 UNAUTHORIZED** `{error: "INVALID_CREDENTIALS"}`.
>
> **Referencia:** `AuthServiceImpl.java:77-104` · `JwtServiceImpl.java:42-76`

---

## 3. Flujo de Registro

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant CTRL as AuthApiController
    participant AUTH as AuthServiceImpl
    participant MAPPER as UserMapper
    participant BC as BCrypt
    participant REPO as UserEntityRepository
    participant DB as PostgreSQL

    C->>+CTRL: POST /api/auth/register {username, email, password}
    CTRL->>+AUTH: createUser(RegisterRequest)
    AUTH->>+REPO: findByEmail(email)
    REPO-->>-AUTH: Optional.empty()
    AUTH->>+MAPPER: toUserEntity(RegisterRequest)
    MAPPER-->>-AUTH: UserEntity (role = USER)
    AUTH->>+BC: encode(password)
    BC-->>-AUTH: "$2a$10$..." (hash)
    AUTH->>AUTH: setPassword(encodedPassword) · setCreatedAt(now)
    AUTH->>+REPO: save(UserEntity)
    REPO->>+DB: INSERT INTO users
    DB-->>-REPO: UserEntity (with UUID)
    REPO-->>-AUTH: UserEntity
    AUTH-->>-CTRL: RegisterResponse
    CTRL-->>-C: 201 CREATED {id, username, email, role: "USER", createdAt}
```

> **Flujo de error:** Si el email ya existe, `findByEmail` devuelve `Optional.of(user)` y el servicio lanza `DuplicateEmailException` que `GlobalExceptionHandler` convierte en **409 CONFLICT** `{error: "USER_ALREADY_EXISTS"}`.
>
> **Referencia:** `AuthServiceImpl.java:44-74` · `UserMapper.java:16` (asigna `UserRole.USER`)

---

## 4. Filtro JWT — Árbol de Decisión

```mermaid
flowchart TD
    START(["Request HTTP entrante"]) --> CHECK_HEADER{"¿Tiene header Authorization?"}

    CHECK_HEADER -->|"NO"| PASS1["doFilter() — Sin autenticar — pasa al siguiente filtro"]
    CHECK_HEADER -->|"SI"| CHECK_BEARER{"¿Empieza con Bearer ?"}

    CHECK_BEARER -->|"NO"| PASS2["doFilter() — No es JWT — permite rutas públicas"]
    CHECK_BEARER -->|"SI"| EXTRACT["Extraer token — Quitar prefijo 'Bearer '"]

    EXTRACT --> PARSE["jwtService.extractEmail(token) — JwtServiceImpl.java:139-147"]
    PARSE -->|"Excepción — Firma inválida / malformado"| LOG_WARN["log.warn('Token JWT inválido')"]
    LOG_WARN --> PASS3["doFilter() — Token corrupto — sin autenticar"]

    PARSE -->|"email válido"| CHECK_CTX{"¿Ya hay autenticación en SecurityContext?"}

    CHECK_CTX -->|"SI — ya autenticado"| PASS4["doFilter() — No sobreescribe autenticación existente"]
    CHECK_CTX -->|"NO"| LOAD_USER["loadUserByUsername(email) — CustomUserDetailsService.java:19-25 — Busca usuario en BD"]

    LOAD_USER --> CHECK_EXPIRED{"¿jwtService.isExpired(token)? — JwtServiceImpl.java:128-137"}

    CHECK_EXPIRED -->|"SI — token expirado"| PASS5["doFilter() — Token caducado — sin autenticar"]
    CHECK_EXPIRED -->|"NO — token válido"| CREATE_AUTH["Crear UsernamePasswordAuthenticationToken — principal = userDetails — credentials = null (JWT ya validado) — authorities = roles del usuario"]

    CREATE_AUTH --> SET_DETAILS["setDetails(request) — IP, sesión, etc."]
    SET_DETAILS --> SET_CTX["SecurityContextHolder.setAuthentication(authToken) — Spring Security 'sabe' quién es el usuario"]
    SET_CTX --> PASS6["doFilter() — Request autenticada — llega al controller"]

    style START fill:#1565c0,stroke:#0d47a1,color:#ffffff
    style CHECK_HEADER fill:#e65100,stroke:#bf360c,color:#ffffff
    style CHECK_BEARER fill:#e65100,stroke:#bf360c,color:#ffffff
    style CHECK_CTX fill:#e65100,stroke:#bf360c,color:#ffffff
    style CHECK_EXPIRED fill:#e65100,stroke:#bf360c,color:#ffffff
    style CREATE_AUTH fill:#2e7d32,stroke:#1b5e20,color:#ffffff
    style SET_CTX fill:#2e7d32,stroke:#1b5e20,color:#ffffff
    style LOG_WARN fill:#b71c1c,stroke:#880e0e,color:#ffffff
    style PASS1 fill:#546e7a,stroke:#37474f,color:#ffffff
    style PASS2 fill:#546e7a,stroke:#37474f,color:#ffffff
    style PASS3 fill:#546e7a,stroke:#37474f,color:#ffffff
    style PASS4 fill:#546e7a,stroke:#37474f,color:#ffffff
    style PASS5 fill:#546e7a,stroke:#37474f,color:#ffffff
    style PASS6 fill:#2e7d32,stroke:#1b5e20,color:#ffffff
```

> **Referencia:** `JwtAuthFilter.java:38-98` — Las 8 secciones numeradas del código

---

## 5. Flujo de Autorización (Request Protegido)

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant FILTER as JwtAuthFilter
    participant JWT_SVC as JwtServiceImpl
    participant UDS as CustomUserDetailsService
    participant CTX as SecurityContext
    participant CTRL as AuthApiController
    participant AUTH_SVC as AuthServiceImpl
    participant ANNO as @PreAuthorize

    C->>+FILTER: GET /api/auth/users Header: Authorization: Bearer eyJhb...
    FILTER->>FILTER: 1. Extraer header Authorization
    FILTER->>FILTER: 2. ¿Empieza con "Bearer "? → SI
    FILTER->>FILTER: 3. Extraer token JWT
    FILTER->>+JWT_SVC: extractEmail(token)
    JWT_SVC-->>-FILTER: "user@email.com"
    FILTER->>+UDS: loadUserByUsername("user@email.com")
    UDS-->>-FILTER: UserEntity (role = USER)
    FILTER->>+JWT_SVC: isExpired(token)
    JWT_SVC-->>-FILTER: false
    FILTER->>FILTER: Crear UsernamePasswordAuthenticationToken
    FILTER->>+CTX: setAuthentication(authToken)
    Note over CTX: SecurityContextHolder ahora sabe que el usuario es USER
    FILTER-->>-FILTER: doFilter()
    FILTER->>+CTRL: GET /api/auth/users
    CTRL->>+AUTH_SVC: listUsers()
    AUTH_SVC->>+ANNO: Verificar @PreAuthorize("hasRole('ADMIN')")
    Note over ANNO: authorities = [ROLE_USER] ¿Tiene ROLE_ADMIN?
    ANNO-->>-AUTH_SVC: Autorizado (USER no tiene ADMIN)
    AUTH_SVC->>AUTH_SVC: repository.findAll()
    AUTH_SVC-->>-CTRL: List of UserSummary
    CTRL-->>-C: 200 OK [{id, username, email, role}]
```

> **Flujo de error:** Si el usuario tiene rol USER, `@PreAuthorize` lanza `AccessDeniedException` que `GlobalExceptionHandler` convierte en **403 FORBIDDEN** `{error: "ACCESS_DENIED"}`. Si no tiene token, retorna **401 UNAUTHORIZED**.
>
> **Referencia:** `AuthServiceImpl.java:119-133` · `UserEntity.java:46-48` · `SecurityConfig.java:24`

---

## 6. Mapa de Cobertura de Tests

```mermaid
graph LR
    subgraph UNIT["TESTS UNITARIOS (sin contexto Spring)"]
        T1["CustomUserDetailsServiceTest — 2 tests"]
        T2["JwtAuthFilterTest — 8 tests"]
        T3["JwtServiceImplTest — 11 tests"]
        T4["AuthServiceImplTest — 6 tests"]
        T5["GlobalExceptionHandlerTest — 6 tests"]
        T6["AuthApiControllerTest — 5 tests"]
        T7["UserEntityTest — 2 tests"]
    end

    subgraph INTEGRATION["TESTS DE INTEGRACIÓN (contexto Spring)"]
        T8["AuthApplicationTests — 1 test — smoke test"]
        T9["AuthServiceMethodSecurityTest — 2 tests — @WithMockUser"]
    end

    subgraph SLICE["TESTS DE REPOSITORIO (data slice)"]
        T10["UserEntityRepositoryTest — 3 tests — H2 in-memory"]
    end

    subgraph COMPONENTS["COMPONENTES CUBIERTOS"]
        C1["CustomUserDetailsService"]
        C2["JwtAuthFilter"]
        C3["JwtServiceImpl"]
        C4["AuthServiceImpl"]
        C5["GlobalExceptionHandler"]
        C6["AuthApiController"]
        C7["UserEntity"]
        C8["Application Context"]
        C9["@PreAuthorize"]
        C10["UserEntityRepository"]
    end

    T1 --> C1
    T2 --> C2
    T3 --> C3
    T4 --> C4
    T5 --> C5
    T6 --> C6
    T7 --> C7
    T8 --> C8
    T9 --> C4
    T9 --> C9
    T10 --> C10

    style UNIT fill:#2e7d32,stroke:#1b5e20,color:#ffffff
    style INTEGRATION fill:#f57f17,stroke:#e65100,color:#ffffff
    style SLICE fill:#1565c0,stroke:#0d47a1,color:#ffffff
    style COMPONENTS fill:#6a1b9a,stroke:#4a148c,color:#ffffff
```

### Resumen de Cobertura

| Capa | Componente | Tests | Tipo | Total |
|------|-----------|-------|------|-------|
| **Config** | `CustomUserDetailsService` | 2 | Unit | |
| **Config** | `JwtAuthFilter` | 8 | Unit | |
| **Service** | `JwtServiceImpl` | 11 | Unit | |
| **Service** | `AuthServiceImpl` | 6 | Unit | |
| **Service** | `@PreAuthorize` | 2 | Integration | |
| **Controller** | `AuthApiController` | 5 | Unit | |
| **Exception** | `GlobalExceptionHandler` | 6 | Unit | |
| **Entity** | `UserEntity` | 2 | Unit | |
| **Repository** | `UserEntityRepository` | 3 | Slice | |
| **Context** | `AuthApplicationTests` | 1 | Integration | |
| | | | **Total** | **46** |

---

## Endpoints de la API

| Método | Ruta | Autenticación | Descripción |
|--------|------|---------------|-------------|
| `POST` | `/api/auth/register` | Púbica | Crear usuario nuevo |
| `POST` | `/api/auth/login` | Pública | Iniciar sesión → devuelve JWT |
| `POST` | `/api/auth/validate` | Pública | Verificar si un token es válido |
| `GET` | `/api/auth/users` | `@PreAuthorize("hasRole('ADMIN')")` | Listar usuarios |

> **Referencia:** `ApiPaths.java:7-22`

---

## Diagrama de Decisión: ¿Qué endpoint uso?

```mermaid
flowchart TD
    START(["¿Qué necesitas hacer?"]) --> Q1{"¿Ya tienes una cuenta?"}

    Q1 -->|"NO"| REG["POST /register — Crea cuenta → devuelve id, username, email"]
    Q1 -->|"SI"| Q2{"¿Tienes un token JWT?"}

    Q2 -->|"NO"| LOGIN["POST /login — email + password → devuelve token"]
    Q2 -->|"SI"| Q3{"¿Qué quieres hacer con el token?"}

    Q3 -->|"Verificar si es válido"| VALIDATE["POST /validate — Header: Authorization: Bearer token"]
    Q3 -->|"Usar para acceder a datos"| Q4{"¿Qué rol tienes?"}

    Q4 -->|"USER"| INFO["Usa el token en Authorize — Puedes validar tu token"]
    Q4 -->|"ADMIN"| USERS["GET /users — Lista todos los usuarios"]

    style START fill:#1565c0,stroke:#0d47a1,color:#ffffff
    style REG fill:#2e7d32,stroke:#1b5e20,color:#ffffff
    style LOGIN fill:#2e7d32,stroke:#1b5e20,color:#ffffff
    style VALIDATE fill:#e65100,stroke:#bf360c,color:#ffffff
    style INFO fill:#6a1b9a,stroke:#4a148c,color:#ffffff
    style USERS fill:#c62828,stroke:#b71c1c,color:#ffffff
```

---

## Configuración por Perfil

```mermaid
graph TB
    subgraph DEV["DEV (default)"]
        D1["PostgreSQL local"]
        D2["JWT secret: hardcoded — dev-secret-key-cambiar-en-produccion"]
        D3["Swagger: HABILITADO OK"]
        D4["Actuator: health + info"]
        D5["Logging: DEBUG"]
    end

    subgraph STG["STAGING"]
        S1["PostgreSQL (env vars)"]
        S2["JWT secret: ${JWT_SECRET}"]
        S3["Swagger: DESHABILITADO X"]
        S4["Actuator: health (detalles)"]
        S5["Logging: INFO"]
    end

    subgraph PROD["PRODUCTION"]
        P1["PostgreSQL (env vars)"]
        P2["JWT secret: ${JWT_SECRET}"]
        P3["Swagger: DESHABILITADO X"]
        P4["Actuator: health (oculto)"]
        P5["Logging: WARN"]
    end

    DEV -.->|"deploy"| STG
    STG -.->|"deploy"| PROD

    style DEV fill:#2e7d32,stroke:#1b5e20,color:#ffffff
    style STG fill:#f57f17,stroke:#e65100,color:#ffffff
    style PROD fill:#c62828,stroke:#b71c1c,color:#ffffff
```

> **Referencia:** `application-dev.yaml` · `application-stg.yaml` · `application-prod.yaml`
