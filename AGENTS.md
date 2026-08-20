# AGENTS.md

Contexto de proyecto para agentes de código (Claude Code, Cursor, etc.). Ver `README.md` para instrucciones de setup orientadas a humanos.

## Qué es esto

Backend Spring Boot (Java 17, Maven) que implementa el flujo OAuth 2.0 + PKCE de Spotify y expone endpoints de solo lectura sobre una playlist. Es un proyecto pequeño/demo, no producción.

## Estructura

```
src/main/java/com/worldWide/spotify/spotifybackend/
├── SpotifyBackendApplication.java
├── config/
│   ├── SecurityConfig.java       # rutas públicas vs autenticadas
│   └── SpotifyProperties.java    # binding de spotify.* en application.properties
├── data/
│   ├── SpotifyPlaylistData.java  # DTO de respuesta de playlist
│   └── SpotifyTokenResponseData.java
├── service/
│   ├── SpotifyAuthService.java   # login/callback/refresh contra accounts.spotify.com
│   └── SpotifyService.java       # llamadas a api.spotify.com/v1
├── util/PKCEUtil.java            # generación de code_verifier / code_challenge
└── web.controller/
    ├── AuthController.java       # /login, /callback
    └── PlaylistController.java   # /playlist, /playlistdata
```

Nota: el paquete `web.controller` tiene un punto literal en el nombre de carpeta (no es un sub-paquete `web.controller`), pero el `package` declarado en los `.java` es `com.worldWide.spotify.spotifybackend.web.controller`. No renombrar la carpeta sin confirmar con el usuario — es una particularidad ya existente del repo.

## Comandos

```bash
./mvnw spring-boot:run   # levantar
./mvnw test               # correr tests
./mvnw compile             # solo compilar
```

Siempre usar `./mvnw`, no asumir que `mvn` global está instalado.

## Convenciones y decisiones ya tomadas

- **Host fijo `127.0.0.1`, no `localhost`**: el `redirect-uri` de Spotify y las cookies de sesión dependen del host exacto. Cualquier código o doc nuevo debe usar `127.0.0.1:8080`.
- **`/login` y `/callback` requieren navegador real**: no se pueden probar por curl/Postman porque Spotify exige login interactivo. Solo se prueban manualmente.
- **`/playlist` vs `/playlistdata`**: mismo resultado, distinta fuente de token (sesión de cookie vs header `Authorization: Bearer`). Es duplicación conocida y aceptada por ahora — no "arreglarla" fusionándolos sin pedir confirmación primero, hay un plan futuro de extender el modelo de playlist antes de tocar esto.
- **`SpotifyPlaylistData` no mapea `tracks.items`**, solo `tracks.total`. Si se pide "traer las canciones", el fix es extender ese modelo (agregar `items` con track/artists/album/duration), no tocar los controllers ni los services.
- **Rutas públicas** viven en `SecurityConfig.java` (`.requestMatchers(...).permitAll()`), incluyendo `/error`. Cualquier endpoint nuevo que no use auth de Spring Security (por ejemplo, que valide su propio Bearer token a mano como `/playlistdata`) debe agregarse ahí explícitamente o Spring Security lo bloqueará con 403 antes de llegar al controller. Como `/error` está permitido, las excepciones no capturadas de los controllers (ej. `IllegalStateException` en `PlaylistController`) terminan en un **500 real de Spring Boot** (`BasicErrorController`, body JSON con `status`/`error`/`message`/`path`), no en un 403 de Spring Security.
- **`@WebMvcTest` no carga `SecurityConfig`**: al ser un slice test, Spring Security cae en su configuración por defecto (todo requiere auth, y `/login` específicamente es interceptado por el login form genérico de Spring Security antes de llegar al controller). Cualquier `@WebMvcTest` sobre un endpoint que en `SecurityConfig` es `permitAll` necesita `@AutoConfigureMockMvc(addFilters = false)` para desactivar los filtros de seguridad y probar el controller real (ver `AuthControllerTest.java` y `SpotifyPlaylistTest.java`).

## CI

`.github/workflows/ci.yml` corre `./mvnw -B verify sonar:sonar` en cada push/PR a `main` (desde `day1-ci-pipeline`/`day2-quality-gates`, ver `context.md` → Historial de avance). No proponer crear un pipeline desde cero — ya existe; si se necesita algo nuevo, extender ese archivo. Incluye gate de cobertura (JaCoCo, mínimo 30% de línea) y análisis estático (SonarCloud, repo público, org `aldenysf`, project key `aldenysf_spotify-backend`, token en el secret `SONAR_TOKEN`). Branch protection no está disponible en este repo (plan Free de GitHub), así que el CI es informativo por ahora, no bloquea merges.

## Credenciales

`spotify.client-secret` y `spotify.current-refresh-token` en `application.properties` se leen desde variables de entorno (`SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REFRESH_TOKEN`), sin default — la app falla al arrancar si faltan. `client-id` sigue en texto plano (no es secreto). Si se toca ese archivo:
- No hardcodear credenciales nuevas ahí.
- No imprimir tokens/secrets completos en logs nuevos (ya existen prints de tokens en `SpotifyAuthService`, son preexistentes, no los repliques en código nuevo).
- Cualquier test nuevo que levante el contexto completo de Spring (`@SpringBootTest`) necesita sus propios valores dummy vía `properties = {...}` en la anotación — no depender de que las env vars reales estén seteadas en CI.

## Testing

- Slice tests (`@WebMvcTest`, mockeados): `SpotifyPlaylistTest.java`, `AuthControllerTest.java`.
- Test de integración (`@SpringBootTest(RANDOM_PORT)` + REST Assured, `SpotifyService` mockeado): `PlaylistDataRestAssuredTest.java`.
- Tests puros con Mockito (sin contexto de Spring): `SpotifyAuthServiceTest.java`.
- La dependencia de Playwright sigue en `pom.xml` (para el Día 5 del plan, E2E), pero **no hay ningún test que la use hoy** — se eliminó `SpotifyAuthControllerTest.java` y `PlaywrightTestCase.java` (tenían cero `@Test` reales, era scaffolding muerto). Si se agrega un test E2E con Playwright, correr primero `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"` para los browsers.

## Qué falta (no asumir que existe)

- No hay endpoints de escritura (agregar/quitar/reordenar canciones).
- No hay endpoint que devuelva el access token en JSON — hoy solo se ve en consola tras `/callback`.
- No hay refresh automático expuesto por HTTP (`SpotifyAuthService.refreshAccessToken()` existe pero no tiene controller que lo llame).
