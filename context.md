# Contexto técnico: spotify-backend (repo Java del plan SDET)

## Qué es
Backend Spring Boot que implementa OAuth 2.0 + PKCE contra la API de Spotify
y expone endpoints de solo lectura sobre una playlist. Proyecto pequeño/demo,
usado como base real para practicar CI/CD, quality gates, API testing y
performance testing.

## Stack
- Java 17, Maven (usa `./mvnw`, wrapper incluido)
- Spring Boot 3.5.4: spring-boot-starter-web, -security, -cache
- Lombok 1.18.30 (⚠️ ver "gaps" — actualmente no se procesa)
- springdoc-openapi (Swagger UI incluido)
- Tests: JUnit 5, Mockito, spring-security-test, Microsoft Playwright (para
  algunos tests E2E existentes)

## Estructura relevante
```
src/main/java/.../spotifybackend/
├── config/SecurityConfig.java       # rutas públicas vs autenticadas
├── config/SpotifyProperties.java    # binding de spotify.* properties
├── data/SpotifyPlaylistData.java    # DTO de respuesta (Lombok @Data)
├── service/SpotifyAuthService.java  # login/callback/refresh
├── service/SpotifyService.java      # llamadas a api.spotify.com
├── util/PKCEUtil.java
└── web.controller/
    ├── AuthController.java          # /login, /callback
    └── PlaylistController.java      # /playlist, /playlistdata
```

## Endpoints
| Método | Ruta | Auth | Notas |
|---|---|---|---|
| GET | /login | pública | inicia OAuth, requiere navegador real |
| GET | /callback | pública | guarda tokens en sesión |
| GET | /playlist | pública, sesión (cookie) | usa token de sesión del navegador |
| GET | /playlistdata | pública, header `Authorization: Bearer` | endpoint stateless, ideal para tests automatizados de API |

Solo lectura — no hay endpoints de escritura (agregar/reordenar canciones).

## Estado actual de testing
- 19 tests unitarios/slice (JUnit + Mockito + `@WebMvcTest`), todos mockeados,
  sin llamadas de red real. Corren con `./mvnw test`.
- No hay pipeline de CI configurado (repo sin `.github/workflows/`).
- No hay medición de cobertura (JaCoCo) ni análisis estático (SonarCloud).
- No hay tests de integración de API con REST Assured (candidato ideal:
  `GET /playlistdata`, que no depende de sesión/cookies).
- No hay tests de performance.
- Repo privado en GitHub: `aldenysf/spotify-backend`.

## Gaps conocidos que son justo material para el plan
- **Día 1-2 (CI + quality gates)**: repo listo para recibir un workflow desde
  cero — no hay nada montado todavía, es terreno limpio.
- **Día 3 (REST Assured)**: `/playlistdata` es el endpoint perfecto — recibe
  Bearer token por header, sin dependencia de sesión de navegador, responde
  JSON simple (`id`, `name`, `tracks.total`, etc.).
- **Día 4 (JMeter)**: mismo endpoint, mockeando o usando un token de prueba,
  sirve como target de carga.
- Bug de build real ya identificado: `maven-compiler-plugin` declara
  `annotationProcessorPaths` solo con `spring-boot-configuration-processor`,
  lo que excluye a Lombok — sus `@Data` no generan getters/setters realmente.
  Pendiente de arreglar (agregar el processor de Lombok al plugin).
- Credenciales de Spotify (client-secret, refresh-token) están en
  `application.properties` en texto plano, commiteadas (repo privado, pero
  a mover a env vars/secrets de CI cuando se arme el pipeline — relevante
  para el Día 1/2, ya que un pipeline real necesita manejar secrets).
- El refresh token de Spotify rota en cada uso (PKCE) — cualquier test que
  dependa de un token hardcodeado se rompe solo; ya se resolvió para los
  tests actuales mockeando la capa de servicio en vez de pegarle a la API real.

## Convención de host
La app siempre corre en `127.0.0.1:8080`, no `localhost` (el redirect-uri de
Spotify y las cookies de sesión están atados a ese host exacto).

## Historial de avance (ir actualizando a medida que se trabaja)
- Fix de OAuth flow, endpoint `/playlistdata` agregado, 403 resuelto en
  `SecurityConfig`, tests rotos arreglados, `SpotifyPlaylistTest` convertido
  a slice test mockeado (`@WebMvcTest`). Ver PR #1.
