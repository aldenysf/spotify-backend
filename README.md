# spotify-backend

Backend en Spring Boot que se autentica contra Spotify (OAuth 2.0 + PKCE) y expone endpoints para leer datos de una playlist.

## Requisitos

- Java 17
- Maven (usa el wrapper incluido `./mvnw`, no hace falta instalar Maven)
- Una app registrada en el [Spotify Developer Dashboard](https://developer.spotify.com/dashboard) con:
  - Redirect URI: `http://127.0.0.1:8080/callback`

## Configuración

La configuración vive en `src/main/resources/application.properties`, bajo el prefijo `spotify.*`:

| Propiedad | Descripción |
|---|---|
| `spotify.client-id` | Client ID de tu app de Spotify |
| `spotify.client-secret` | Client secret de tu app de Spotify |
| `spotify.redirect-uri` | Debe coincidir exacto con el configurado en el Dashboard de Spotify |
| `spotify.scopes` | Scopes de OAuth pedidos en el login |
| `spotify.playlist-id` | ID de la playlist que se consulta en `/playlist` y `/playlistdata` |
| `spotify.base-url` | Base URL de la API de Spotify (`https://api.spotify.com/v1`) |
| `spotify.current-refresh-token` | Refresh token usado como fallback para renovar el access token sin pasar por `/login` |

`spotify.client-secret` y `spotify.current-refresh-token` se leen desde variables de entorno (`SPOTIFY_CLIENT_SECRET`, `SPOTIFY_REFRESH_TOKEN`) — no hay valores por defecto, así que la app falla al arrancar si no están seteadas. Configúralas antes de levantar el proyecto:

```bash
export SPOTIFY_CLIENT_SECRET=tu-client-secret
export SPOTIFY_REFRESH_TOKEN=tu-refresh-token
```

En IntelliJ: agrégalas en la Run Configuration (Environment variables).

## Cómo levantarlo

```bash
./mvnw spring-boot:run
```

La app queda escuchando en `http://127.0.0.1:8080`. Usa siempre `127.0.0.1` y no `localhost` — el `redirect-uri` de Spotify y las cookies de sesión están atadas a ese host exacto.

Si el puerto 8080 ya está ocupado (por ejemplo, una corrida anterior que quedó viva), busca el proceso y mátalo:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
kill <PID>
```

## Flujo de autenticación

1. Abre `http://127.0.0.1:8080/login` **en el navegador**. Te redirige a la pantalla de login real de Spotify.
2. Tras aceptar, Spotify redirige a `/callback`, que intercambia el código por `access_token`/`refresh_token` y los guarda en la sesión del navegador.
3. El access token queda impreso en la consola de la app (`Nuevo access token obtenido: ...`). Los tokens de Spotify expiran en ~1 hora; si expira, repite el login.

## Endpoints

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `GET` | `/login` | Pública | Inicia el flujo OAuth (abrir en navegador) |
| `GET` | `/callback` | Pública | Callback de Spotify, guarda tokens en sesión |
| `GET` | `/playlist` | Pública, usa sesión del navegador | Devuelve los datos de la playlist configurada usando el token de la sesión |
| `GET` | `/playlistdata` | Pública, requiere header `Authorization: Bearer <token>` | Igual que `/playlist`, pero sin depender de cookies — pensado para Postman/clientes externos |

Cualquier otra ruta requiere autenticación (usuario/contraseña que Spring Security genera al arrancar, ver consola: `Using generated security password: ...`).

### Probar `/playlist` en el navegador

Después de loguearte, abre en la **misma pestaña**:

```
http://127.0.0.1:8080/playlist
```

### Probar `/playlistdata` en Postman

1. Copia el access token de la consola.
2. `GET http://127.0.0.1:8080/playlistdata`
3. Pestaña **Authorization** → Type: **Bearer Token** → pega el token.

## Tests

```bash
./mvnw test
```

## Performance testing (JMeter)

```bash
./mvnw -Pperf clean verify
```

Corre un load test (50 usuarios, 500 requests) contra `/playlistdata` con un stub de `SpotifyService` (sin depender de la API real de Spotify). No hace falta tener JMeter instalado — el plugin lo descarga vía Maven. Reporte HTML en `target/jmeter/reports/playlistdata-load-test/index.html`. Usa siempre `clean`, el reporte no se sobreescribe entre corridas.

## Limitaciones conocidas

- No hay endpoints de escritura (agregar/quitar canciones, reordenar, etc.), solo lectura.
- `/playlist` y `/playlistdata` devuelven el mismo shape de datos y comparten la misma lógica de servicio; solo difieren en de dónde sacan el token.
- El modelo `SpotifyPlaylistData` no mapea el listado de canciones (`tracks.items`), solo el total.
