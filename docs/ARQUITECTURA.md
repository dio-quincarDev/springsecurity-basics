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
        CSRF["CSRF<br/><i>Desactivado — API REST stateless</i>"]
        SESSION["Sesiones<br/><i>STATELESS — sin cookies</i>"]
        JWT_FILTER["JwtAuthFilter<br/><i>OncePerRequestFilter</i><br/>Extrae · Valida · Autentica"]
    end

    subgraph SECURITY_CONFIG["CONFIGURACIÓN"]
        SC["SecurityFilterChain<br/><i>SecurityConfig.java:33-65</i>"]
        AUTH_PROVIDER["DaoAuthenticationProvider<br/><i>UserDetailsService + BCrypt</i>"]
        AUTH_MANAGER["AuthenticationManager<br/><i>Orquesta la autenticación</i>"]
    end

    subgraph CONTROLLER["CAPA CONTROLLER"]
        CTRL["AuthApiController<br/><i>POST /register · /login · /validate<br/>GET /users</i>"]
    end

    subgraph SERVICE["CAPA SERVICE"]
        AUTH_SVC["AuthServiceImpl<br/><i>· @PreAuthorize('hasRole(ADMIN)')<br/>· BCrypt · Login · Validación</i>"]
        JWT_SVC["JwtServiceImpl<br/><i>· generateToken() — HMAC-SHA<br/>· validateToken() — firma + expiración<br/>· extractEmail() / extractRole()</i>"]
    end

    subgraph REPOSITORY["CAPA REPOSITORY"]
        REPO["UserEntityRepository<br/><i>Spring Data JPA — findByEmail()</i>"]
    end

    subgraph DATABASE["BASE DE DATOS"]
        DB["PostgreSQL<br/><i>Tabla: users (UUID, email, password, role)</i>"]
    end

    subgraph EXCEPTIONS["MANEJO DE ERRORES"]
        GEX["GlobalExceptionHandler<br/><i>401 · 403 · 409 · 400</i>"]
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

    style CLIENT fill:#e3f2fd,stroke:#1565c0,color:#0d47a1
    style FILTER_CHAIN fill:#fff3e0,stroke:#e65100,color:#bf360c
    style SECURITY_CONFIG fill:#fce4ec,stroke:#c62828,color:#b71c1c
    style CONTROLLER fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style SERVICE fill:#f3e5f5,stroke:#6a1b9a,color:#4a148c
    style REPOSITORY fill:#fff8e1,stroke:#f57f17,color:#e65100
    style DATABASE fill:#eceff1,stroke:#37474f,color:#263238
    style EXCEPTIONS fill:#ffebee,stroke:#c62828,color:#b71c1c
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

    C->>+CTRL: POST /api/auth/login<br/>{email, password}
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
    JWT->>JWT: Construir JWT con claims<br/>{sub, userId, username, role}<br/>+ firma HMAC-SHA
    JWT-->>-AUTH: TokenResponse
    AUTH-->>-CTRL: TokenResponse
    CTRL-->>-C: 200 OK<br/>{token, tokenType: "Bearer",<br/>expiresIn: 3600, userId}
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

    C->>+CTRL: POST /api/auth/register<br/>{username, email, password}
    CTRL->>+AUTH: createUser(RegisterRequest)
    AUTH->>+REPO: findByEmail(email)
    REPO-->>-AUTH: Optional.empty()
    AUTH->>+MAPPER: toUserEntity(RegisterRequest)
    MAPPER-->>-AUTH: UserEntity (role = USER)
    AUTH->>+BC: encode(password)
    BC-->>-AUTH: "$2a$10$..." (hash)
    AUTH->>AUTH: setPassword(encodedPassword)<br/>setCreatedAt(now)
    AUTH->>+REPO: save(UserEntity)
    REPO->>+DB: INSERT INTO users
    DB-->>-REPO: UserEntity (with UUID)
    REPO-->>-AUTH: UserEntity
    AUTH-->>-CTRL: RegisterResponse
    CTRL-->>-C: 201 CREATED<br/>{id, username, email,<br/>role: "USER", createdAt}
```

> **Flujo de error:** Si el email ya existe, `findByEmail` devuelve `Optional.of(user)` y el servicio lanza `DuplicateEmailException` que `GlobalExceptionHandler` convierte en **409 CONFLICT** `{error: "USER_ALREADY_EXISTS"}`.
>
> **Referencia:** `AuthServiceImpl.java:44-74` · `UserMapper.java:16` (asigna `UserRole.USER`)

---

## 4. Filtro JWT — Árbol de Decisión

```mermaid
flowchart TD
    START(["Request HTTP entrante"]) --> CHECK_HEADER{"¿Tiene header<br/>Authorization?"}

    CHECK_HEADER -->|"NO"| PASS1["doFilter()<br/><i>Sin autenticar — pasa al siguiente filtro</i>"]
    CHECK_HEADER -->|"SI"| CHECK_BEARER{"¿Empieza con<br/>Bearer ?"}

    CHECK_BEARER -->|"NO"| PASS2["doFilter()<br/><i>No es JWT — permite rutas públicas</i>"]
    CHECK_BEARER -->|"SI"| EXTRACT["Extraer token<br/><i>Quitar prefijo 'Bearer '</i>"]

    EXTRACT --> PARSE["jwtService.extractEmail(token)<br/><i>JwtServiceImpl.java:139-147</i>"]
    PARSE -->|"Excepción<br/><i>Firma inválida / malformado</i>"| LOG_WARN["log.warn('Token JWT inválido')"]
    LOG_WARN --> PASS3["doFilter()<br/><i>Token corrupto — sin autenticar</i>"]

    PARSE -->|"email válido"| CHECK_CTX{"¿Ya hay autenticación<br/>en SecurityContext?"}

    CHECK_CTX -->|"SI — ya autenticado"| PASS4["doFilter()<br/><i>No sobreescribe autenticación existente</i>"]
    CHECK_CTX -->|"NO"| LOAD_USER["loadUserByUsername(email)<br/><i>CustomUserDetailsService.java:19-25</i><br/>Busca usuario en BD"]

    LOAD_USER --> CHECK_EXPIRED{"¿jwtService.isExpired(token)?<br/><i>JwtServiceImpl.java:128-137</i>"}

    CHECK_EXPIRED -->|"SI — token expirado"| PASS5["doFilter()<br/><i>Token caducado — sin autenticar</i>"]
    CHECK_EXPIRED -->|"NO — token válido"| CREATE_AUTH["Crear UsernamePasswordAuthenticationToken<br/><i>principal = userDetails<br/>credentials = null (JWT ya validado)<br/>authorities = roles del usuario</i>"]

    CREATE_AUTH --> SET_DETAILS["setDetails(request)<br/><i>IP, sesión, etc.</i>"]
    SET_DETAILS --> SET_CTX["SecurityContextHolder<br/>.setAuthentication(authToken)<br/><i>Spring Security 'sabe' quién es el usuario</i>"]
    SET_CTX --> PASS6["doFilter()<br/><i>Request autenticada — llega al controller</i>"]

    style START fill:#e3f2fd,stroke:#1565c0,color:#0d47a1
    style CHECK_HEADER fill:#fff3e0,stroke:#e65100,color:#bf360c
    style CHECK_BEARER fill:#fff3e0,stroke:#e65100,color:#bf360c
    style CHECK_CTX fill:#fff3e0,stroke:#e65100,color:#bf360c
    style CHECK_EXPIRED fill:#fff3e0,stroke:#e65100,color:#bf360c
    style CREATE_AUTH fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style SET_CTX fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style LOG_WARN fill:#ffebee,stroke:#c62828,color:#b71c1c
    style PASS1 fill:#eceff1,stroke:#37474f,color:#263238
    style PASS2 fill:#eceff1,stroke:#37474f,color:#263238
    style PASS3 fill:#eceff1,stroke:#37474f,color:#263238
    style PASS4 fill:#eceff1,stroke:#37474f,color:#263238
    style PASS5 fill:#eceff1,stroke:#37474f,color:#263238
    style PASS6 fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
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

    C->>+FILTER: GET /api/auth/users<br/>Header: Authorization: Bearer eyJhb...
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
    Note over CTX: SecurityContextHolder ahora<br/>sabe que el usuario es USER
    FILTER-->>-FILTER: doFilter()
    FILTER->>+CTRL: GET /api/auth/users
    CTRL->>+AUTH_SVC: listUsers()
    AUTH_SVC->>+ANNO: Verificar @PreAuthorize("hasRole('ADMIN')")
    Note over ANNO: authorities = [ROLE_USER]<br/>¿Tiene ROLE_ADMIN?
    ANNO-->>-AUTH_SVC: Autorizado (USER no tiene ADMIN)
    AUTH_SVC->>AUTH_SVC: repository.findAll()
    AUTH_SVC-->>-CTRL: List of UserSummary
    CTRL-->>-C: 200 OK<br/>[{id, username, email, role}]
```

> **Flujo de error:** Si el usuario tiene rol USER, `@PreAuthorize` lanza `AccessDeniedException` que `GlobalExceptionHandler` convierte en **403 FORBIDDEN** `{error: "ACCESS_DENIED"}`. Si no tiene token, retorna **401 UNAUTHORIZED**.
>
> **Referencia:** `AuthServiceImpl.java:119-133` · `UserEntity.java:46-48` · `SecurityConfig.java:24`

---

## 6. Mapa de Cobertura de Tests

```mermaid
graph LR
    subgraph UNIT["TESTS UNITARIOS (sin contexto Spring)"]
        T1["CustomUserDetailsServiceTest<br/><i>2 tests</i>"]
        T2["JwtAuthFilterTest<br/><i>8 tests</i>"]
        T3["JwtServiceImplTest<br/><i>11 tests</i>"]
        T4["AuthServiceImplTest<br/><i>6 tests</i>"]
        T5["GlobalExceptionHandlerTest<br/><i>6 tests</i>"]
        T6["AuthApiControllerTest<br/><i>5 tests</i>"]
        T7["UserEntityTest<br/><i>2 tests</i>"]
    end

    subgraph INTEGRATION["TESTS DE INTEGRACIÓN (contexto Spring)"
]
        T8["AuthApplicationTests<br/><i>1 test — smoke test</i>"]
        T9["AuthServiceMethodSecurityTest<br/><i>2 tests — @WithMockUser</i>"]
    end

    subgraph SLICE["TESTS DE REPOSITORIO (data slice)"]
        T10["UserEntityRepositoryTest<br/><i>3 tests — H2 in-memory</i>"]
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

    style UNIT fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style INTEGRATION fill:#fff8e1,stroke:#f57f17,color:#e65100
    style SLICE fill:#e3f2fd,stroke:#1565c0,color:#0d47a1
    style COMPONENTS fill:#f3e5f5,stroke:#6a1b9a,color:#4a148c
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
    START(["¿Qué necesitas hacer?"]) --> Q1{"¿Ya tienes<br/>una cuenta?"}

    Q1 -->|"NO"| REG["POST /register<br/><i>Crea cuenta → devuelve id, username, email</i>"]
    Q1 -->|"SI"| Q2{"¿Tienes un<br/>token JWT?"}

    Q2 -->|"NO"| LOGIN["POST /login<br/><i>email + password → devuelve token</i>"]
    Q2 -->|"SI"| Q3{"¿Qué quieres<br/>hacer con el token?"}

    Q3 -->|"Verificar si es válido"| VALIDATE["POST /validate<br/><i>Header: Authorization: Bearer token</i>"]
    Q3 -->|"Usar para acceder a datos"| Q4{"¿Qué rol tienes?"}

    Q4 -->|"USER"| INFO["Usa el token en Authorize<br/><i>Puedes validar tu token</i>"]
    Q4 -->|"ADMIN"| USERS["GET /users<br/><i>Lista todos los usuarios</i>"]

    style START fill:#e3f2fd,stroke:#1565c0,color:#0d47a1
    style REG fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style LOGIN fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style VALIDATE fill:#fff3e0,stroke:#e65100,color:#bf360c
    style INFO fill:#f3e5f5,stroke:#6a1b9a,color:#4a148c
    style USERS fill:#fce4ec,stroke:#c62828,color:#b71c1c
```

---

## Configuración por Perfil

```mermaid
graph TB
    subgraph DEV["DEV (default)"]
        D1["PostgreSQL local"]
        D2["JWT secret: hardcoded<br/><i>dev-secret-key-cambiar-en-produccion</i>"]
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

    style DEV fill:#e8f5e9,stroke:#2e7d32,color:#1b5e20
    style STG fill:#fff8e1,stroke:#f57f17,color:#e65100
    style PROD fill:#ffebee,stroke:#c62828,color:#b71c1c
```

> **Referencia:** `application-dev.yaml` · `application-stg.yaml` · `application-prod.yaml`
