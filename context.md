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
- Tests: JUnit 5, Mockito, spring-security-test, REST Assured. Playwright
  sigue como dependencia (para el Día 5, E2E), pero hoy no la usa ningún test.

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
- 21 tests (18 unitarios/slice mockeados + 2 de integración con REST Assured
  sobre `/playlistdata` + `SpotifyAuthServiceTest` con Mockito puro), sin
  llamadas de red real. Corren con `./mvnw test`. Cero tests vacíos/muertos
  (se eliminó `SpotifyAuthControllerTest.java` y `PlaywrightTestCase.java`,
  que no tenían ningún `@Test` real).
- CI: `.github/workflows/ci.yml` corre `./mvnw -B verify sonar:sonar` en cada
  push/PR a `main` (Día 1 + 2 + 3). *Branch protection no disponible en este
  repo (plan Free de GitHub) — el CI es informativo, no bloquea merges
  todavía.*
- Cobertura: JaCoCo mide y bloquea el build si la cobertura de línea del
  bundle baja de 30% (medido real al Día 3: **54.8%**, subió desde 35.2% del
  Día 2 gracias a los tests de REST Assured y al de `AuthController.login`).
  Reporte HTML se sube como artefacto del workflow (`jacoco-report`).
- Análisis estático: SonarCloud conectado (repo público desde el Día 2).
  Organization `aldenysf`, project key `aldenysf_spotify-backend`. Token en
  el secret `SONAR_TOKEN` del repo (GitHub Actions).
- REST Assured: `PlaylistDataRestAssuredTest.java` — `@SpringBootTest(webEnvironment = RANDOM_PORT)`
  con `SpotifyService` mockeado, cubre Bearer válido (200 + shape del JSON) y
  sin header (**500** con body de error completo — `SecurityConfig` ahora
  permite `/error`, así que Spring Boot arma un 500 real en vez del 403 que
  daba antes de ese cambio).
- `AuthControllerTest.java` (`/login`) ya no está vacío: valida el redirect a
  la URL de Spotify (mockeada) y que `code_verifier`/`state` queden en
  sesión. Necesita `@AutoConfigureMockMvc(addFilters = false)` porque
  `@WebMvcTest` no carga `SecurityConfig` y `/login` choca con el login form
  por defecto de Spring Security.
- Performance: JMeter vía `jmeter-maven-plugin` (no requiere instalar JMeter
  en el sistema, lo descarga Maven). Profile opcional `perf`, no toca el
  pipeline normal. Ver sección "Performance testing" más abajo.
- No hay E2E con Playwright en CI (Día 5) — diferido: mejor abordarlo cuando
  se decida el scope real (login interactivo de Spotify es difícil de
  automatizar de punta a punta).
- Repo público en GitHub: `aldenysf/spotify-backend`.

## Performance testing (Día 4)
- Profile Maven `perf` (no activo por defecto): `./mvnw -Pperf clean verify`.
  Local: usar siempre `clean` antes, el reporte HTML de JMeter no sobreescribe
  una carpeta no vacía de una corrida anterior.
- Flujo: `spring-boot:start` levanta la app real en el puerto 8089 con el
  profile Spring `perf` activo (`--spring.profiles.active=perf`) y secrets
  dummy de Spotify (no hacen falta reales) → `jmeter-maven-plugin` corre
  `src/test/jmeter/playlistdata-load-test.jmx` contra `127.0.0.1:8089` →
  `spring-boot:stop` apaga la app.
- **Por qué un profile Spring aparte**: con el profile `perf` activo,
  `PerfStubSpotifyService` (en `config/`, `@Profile("perf") @Primary`)
  reemplaza a `SpotifyService` y devuelve datos fijos al instante — así JMeter
  mide el rendimiento real de esta app (Controller + Security + Tomcat) sin
  pegarle a la API real de Spotify con carga concurrente (rate limits, token
  que rota, etc.).
- Plan de carga: 50 threads, ramp-up 10s, 10 loops cada uno (500 requests
  totales) contra `GET /playlistdata` con header Bearer fijo. Assertions de
  response code 200 y tiempo de respuesta bajo 1000ms.
- **Resultado real medido**: 500/500 requests exitosos, 0% error rate,
  throughput ~50.8 req/s, latencia promedio 2ms (pico inicial de ~130ms por
  warm-up de JVM/Tomcat en la primera request). Reporte HTML en
  `target/jmeter/reports/playlistdata-load-test/index.html` (gitignored,
  se regenera en cada corrida).
- CI: `.github/workflows/performance.yml`, trigger manual
  (`workflow_dispatch`, no corre en cada push) — corre `./mvnw -B -Pperf clean verify`
  y sube el reporte HTML como artefacto (`jmeter-report`).

## Gaps conocidos que son justo material para el plan
- **Día 2 (quality gates)**: completo — JaCoCo (gate en 30% de línea) y
  SonarCloud (análisis estático) ya implementados.
- **Día 3 (REST Assured)**: completo — ver `PlaylistDataRestAssuredTest.java`.
- **Día 4 (JMeter)**: completo — ver sección "Performance testing".
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
  real medida: 35.2% de líneas. Repo pasado a público, conectado a SonarCloud
  (`sonar-maven-plugin`, org `aldenysf`, project key
  `aldenysf_spotify-backend`, token en secret `SONAR_TOKEN`) — `ci.yml` corre
  `./mvnw -B verify sonar:sonar` con `fetch-depth: 0` en el checkout. Branch:
  `day2-quality-gates`.
- **Día 3**: agregado `rest-assured` (test scope) y
  `PlaylistDataRestAssuredTest.java` — `@SpringBootTest(RANDOM_PORT)` con
  `SpotifyService` mockeado, dos tests sobre `/playlistdata`. Además:
  `SecurityConfig` ganó `permitAll("/error")`, así que sin header
  Authorization ahora responde **500 real** (no 403) — el test quedó
  actualizado a eso. Se rellenó `AuthControllerTest.java` (estaba vacío, sin
  aserciones) y se eliminó `SpotifyAuthControllerTest.java` +
  `PlaywrightTestCase.java` (scaffolding de Playwright sin ningún `@Test`
  real). Cobertura subió a 54.8% de líneas. Branch:
  `day3-rest-assured-api-tests`.
- **Día 4**: agregado profile Maven `perf` (spring-boot:start/stop +
  jmeter-maven-plugin, sin necesidad de instalar JMeter en el sistema).
  Nuevo `PerfStubSpotifyService` (`@Profile("perf") @Primary`) para no
  depender de la API real de Spotify durante la carga. Plan de carga
  `src/test/jmeter/playlistdata-load-test.jmx` (50 threads, ramp-up 10s, 500
  requests). Resultado real: 0% error rate, ~50.8 req/s, 2ms de latencia
  promedio. Workflow separado `performance.yml` (manual, no en cada push).
  Cobertura: 60.5% de líneas (se agregó `PerfStubSpotifyServiceTest`).
  Branch: `day4-jmeter-performance-testing`.
