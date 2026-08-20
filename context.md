# Contexto técnico: spotify-backend (repo Java del plan SDET)

## Qué es
Backend Spring Boot que implementa OAuth 2.0 + PKCE contra la API de Spotify
y expone endpoints de solo lectura sobre una playlist. Proyecto pequeño/demo,
usado como base real para practicar CI/CD, quality gates, API testing y
performance testing.

## Stack
- Java 17, Maven (usa `./mvnw`, wrapper incluido)
- Spring Boot 3.5.4: spring-boot-starter-web, -security, -cache
- Lombok 1.18.30 (procesado correctamente desde el Día 1 — ver Historial)
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
- CI: `.github/workflows/ci.yml` corre `./mvnw -B verify` en cada push/PR a
  `main` (Día 1 + 2). *Branch protection no disponible en este repo privado
  (plan Free de GitHub) — el CI es informativo, no bloquea merges todavía.*
- Cobertura: JaCoCo mide y bloquea el build si la cobertura de línea del
  bundle baja de 30% (medido real al Día 2: ~35.2%). Reporte HTML se sube como
  artefacto del workflow (`jacoco-report`). Análisis estático (SonarCloud)
  sigue diferido: requiere repo público o Docker local, ninguno
  decidido/disponible por ahora.
- No hay tests de integración de API con REST Assured (candidato ideal:
  `GET /playlistdata`, que no depende de sesión/cookies) — pendiente Día 3.
- No hay tests de performance (JMeter, Día 4) ni E2E con Playwright en CI
  (Día 5) — diferidos: JMeter/Docker no están instalados en la máquina de
  desarrollo actual, mejor implementarlos cuando estén disponibles para poder
  verificarlos de verdad.
- Repo privado en GitHub: `aldenysf/spotify-backend`.

## Gaps conocidos que son justo material para el plan
- **Día 2 (quality gates)**: JaCoCo ya implementado (gate en 30% de línea).
  Falta Sonar (análisis estático), diferido.
- **Día 3 (REST Assured)**: `/playlistdata` es el endpoint perfecto — recibe
  Bearer token por header, sin dependencia de sesión de navegador, responde
  JSON simple (`id`, `name`, `tracks.total`, etc.). Ojo: sin header
  `Authorization`, el endpoint responde **403** (no 500) bajo Tomcat real —
  Spring Security intercepta el forward a `/error` porque `SecurityConfig` no
  tiene `permitAll` para esa ruta.
- **Día 4 (JMeter)**: mismo endpoint, mockeando o usando un token de prueba,
  sirve como target de carga.
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
- **Día 1**: fix de Lombok (`annotationProcessorPaths` ahora incluye
  `org.projectlombok:lombok`), secrets de Spotify movidos a variables de
  entorno (`SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REFRESH_TOKEN`, sin default —
  la app falla rápido si faltan), y `.github/workflows/ci.yml` corriendo
  `./mvnw -B test` en cada push/PR a `main`. Branch: `day1-ci-pipeline`.
- **Día 2**: agregado `jacoco-maven-plugin` con gate de cobertura de línea
  ≥30% a nivel `BUNDLE` (falla `verify` si baja). `ci.yml` pasa a correr
  `./mvnw -B verify` y sube el reporte HTML de JaCoCo como artefacto. Cobertura
  real medida: 35.2% de líneas. Sonar queda diferido (repo privado, sin
  Docker). Branch: `day2-quality-gates`.
