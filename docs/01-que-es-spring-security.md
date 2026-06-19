# Tutorial 1: Que es Spring Security

> **Tiempo estimado:** 15 minutos
> **Prerequisito:** Conocimientos basicos de Java y Spring Boot
> **Archivos clave:** `SecurityConfig.java`, `JwtAuthFilter.java`

---

## 1. El problema que resuelve

Sin un framework de seguridad, cada desarrollador tendria que resolver manualmente:
- Quien puede acceder a cada endpoint
- Como verificar contraseñas
- Como manejar sesiones o tokens
- Que hacer cuando alguien no esta autorizado

**Spring Security** resuelve todo esto con un sistema de **filtros** que interceptan cada request HTTP antes de que llegue a tu controlador.

---

## 2. La cadena de filtros (Filter Chain)

Imagina una aduana de aeropuerto. Cada pasajero (request) pasa por plusieurs estaciones de control antes de llegar al avion (tu controlador):

```
Request HTTP
    │
    ▼
[Filtro 1] ¿Tiene pasaporte? ─── NO ──→ Rechazado
    │ SI
    ▼
[Filtro 2] ¿El pasaporte es valido? ─── NO ──→ Rechazado
    │ SI
    ▼
[Filtro 3] ¿Esta en la lista negra? ─── SI ──→ Rechazado
    │ NO
    ▼
Tu Controlador (el avion)
```

En Spring Security, esta cadena esta configurada en `SecurityConfig.java:33-65`:

```java
// SecurityConfig.java:33-65
http
    .csrf(AbstractHttpConfigurer::disable)                    // Filtro: desactivar CSRF
    .sessionManagement(session -> session
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Filtro: no crear sesiones
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/register").permitAll()    // Filtro: rutas publicas
        .requestMatchers("/api/auth/login").permitAll()
        .anyRequest().authenticated())                         // Filtro: todo lo demas requiere auth
    .addFilterBefore(jwtAuthFilter,                           // Filtro: nuestro JWT
        UsernamePasswordAuthenticationFilter.class);
```

Cada linea es una regla que Spring Security evalua en orden.

---

## 3. JWT vs Sesiones

Hay dos formas de manejar la autenticacion:

| | Sesiones (tradicional) | JWT (nuestro proyecto) |
|---|---|---|
| **Donde se guarda el estado** | En el servidor (memoria/BD) | En el cliente (header HTTP) |
| **Escalabilidad** | Dificil (cada servidor tiene su propia sesion) | Facil (cualquier servidor puede validar el token) |
| **Cookies** | Si (el navegador las envia automaticamente) | No (el cliente envia el header manualmente) |
| **Estado del servidor** | Guarda quien esta logueado | No guarda nada (stateless) |

Nuestro proyecto usa JWT porque es una API REST que debe ser **stateless**: el servidor no recuerda quién esta logueado. Cada request trae su propio token.

```
Sesiones:    Cliente ──cookie──> Servidor (recuerda al usuario)
JWT:         Cliente ──header──> Servidor (valida el token en cada request)
```

---

## 4. Como encaja JWT en la cadena

El filtro JWT (`JwtAuthFilter.java`) se agrega **antes** del filtro de login por formulario:

```java
// SecurityConfig.java:59-62
// Agregamos nuestro filtro JWT ANTES del filtro de login por formulario.
// Asi, cada request pasa por JwtAuthFilter, que extrae el token del header,
// lo valida y coloca la autenticacion en el contexto de seguridad.
.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

Esto significa que **cada request** pasa por nuestro filtro, sin importar si es publica o protegida. El filtro decide si autenticar o no.

---

## 5. Los tres actores principales

| Componente | Archivo | Que hace |
|-----------|---------|----------|
| **SecurityFilterChain** | `SecurityConfig.java:33-65` | Define las reglas: que rutas son publicas, que filtros se usan |
| **JwtAuthFilter** | `JwtAuthFilter.java:30-98` | Extrae el token, lo valida, y autentica al usuario |
| **AuthenticationManager** | `SecurityConfig.java:76-79` | Orquesta la autenticacion (login) delegando en providers |

---

## Diagrama de referencia

Ver **Arquitectura General** en `docs/ARQUITECTURA.md` (diagrama 1).

---

## Ejercicio

Abre `SecurityConfig.java` y responde:

1. Que anotaciones tiene la clase? Que hace cada una?
2. Que rutas estan en `PUBLIC_AUTH_ENDPOINTS`? (Busca `ApiPaths.java`)
3. Que pasa si agregas una ruta nueva a `.requestMatchers("/api/auth/debug").permitAll()`? La ruta estaria protegida o no?

---

## Preguntas de verificacion

1. Que es un "filter chain" en Spring Security?
2. Por que desactivamos CSRF en una API REST con JWT?
3. Que diferencia hay entre una sesion y un JWT?
4. En que archivo se define que endpoints son publicos?
5. Que pasaria si eliminamos el `.addFilterBefore(jwtAuthFilter, ...)` de SecurityConfig?

---

## Siguiente

Siguiente tutorial: [02 - Modelo de datos](02-modelo-de-datos.md)
