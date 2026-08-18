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
- **Rutas públicas** viven en `SecurityConfig.java` (`.requestMatchers(...).permitAll()`). Cualquier endpoint nuevo que no use auth de Spring Security (por ejemplo, que valide su propio Bearer token a mano como `/playlistdata`) debe agregarse ahí explícitamente o Spring Security lo bloqueará con 403 antes de llegar al controller.

## Credenciales

`application.properties` tiene actualmente `client-id`, `client-secret` y un `current-refresh-token` reales en texto plano, commiteados al repo (repo privado). Si se toca ese archivo:
- No hardcodear credenciales nuevas ahí.
- No imprimir tokens/secrets completos en logs nuevos (ya existen prints de tokens en `SpotifyAuthService`, son preexistentes, no los repliques en código nuevo).

## Testing

- `SpotifyPlaylistTest.java`, `SpotifyAuthControllerTest.java`, `SpotifyAuthServiceTest.java` bajo `src/test`.
- Hay dependencia de Playwright (`web/fixtures/PlaywrightTestCase.java`) para algún test que dirige un navegador real — si un test falla por falta de browsers de Playwright, correr `mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"` en vez de asumir que es un bug de código.

## Qué falta (no asumir que existe)

- No hay endpoints de escritura (agregar/quitar/reordenar canciones).
- No hay endpoint que devuelva el access token en JSON — hoy solo se ve en consola tras `/callback`.
- No hay refresh automático expuesto por HTTP (`SpotifyAuthService.refreshAccessToken()` existe pero no tiene controller que lo llame).
