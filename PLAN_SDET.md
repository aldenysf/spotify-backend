# PLAN_SDET — spotify-backend

Plan de trabajo para convertir este repo en mi campo de práctica de CI/CD, quality
gates, API testing y performance testing. Objetivo doble: subir de nivel técnico y
tener material fresco y verificable para hablar en entrevistas (meta salarial: de
1.8M a 2.3M+ CLP como QA Automation / SDET).

## Cómo trabajar este plan con Claude en IntelliJ

1. Este archivo vive en la raíz del repo, junto a `context.md`.
2. Al empezar una sesión, decirle a Claude: **"lee `context.md` y `PLAN_SDET.md`;
   estoy en el Día N, ayúdame con X."**
3. Al cerrar cada día, actualizar la sección "Historial de avance" de `context.md`
   con lo que quedó hecho (commits, PRs, métricas).

## Datos del repo que importan para el plan
- Java 17, Maven con wrapper: usar siempre `./mvnw` (no `mvn` global).
- La app corre en `127.0.0.1:8080` (NO `localhost`): el redirect-uri de Spotify y
  las cookies están atados a ese host exacto.
- Endpoint clave para testing automatizado: **`GET /playlistdata`** — es stateless,
  recibe `Authorization: Bearer <token>` por header, no depende de sesión/cookie.
- Estado inicial: 19 tests unitarios/slice mockeados, sin CI, sin cobertura, sin
  análisis estático, sin API tests de integración, sin performance.

---

## Pre-trabajo (antes del Día 1)

### A) Rotar y sacar los secrets de Spotify (seguridad — hacerlo primero)
- El `client-secret` y el `refresh-token` están commiteados en `application.properties`
  y quedaron en el historial de git. Tratarlos como expuestos.
- Rotar el `client-secret` en el **Spotify Developer Dashboard**.
- Mover la config a variables de entorno (Spring las lee vía `${SPOTIFY_CLIENT_SECRET}`
  etc.) y, para CI, a **GitHub Actions secrets**.
- Nota tranquilizadora: los 19 tests están mockeados y no pegan a la red real, así que
  el pipeline de CI **no necesita estos secrets** para correr.

### B) Verificar Lombok (lo confirma el propio pipeline)
- `annotationProcessorPaths` del `maven-compiler-plugin` solo declara
  `spring-boot-configuration-processor`, lo que puede estar excluyendo a Lombok.
- Si el Día 1 el build falla al compilar, agregar al `annotationProcessorPaths`:
  `org.projectlombok:lombok` (con su versión). Si pasa, Lombok se resuelve por otra
  vía y no hay que tocar nada.

---

## Día 1 — Primer pipeline de CI (GitHub Actions)
- **Construir:** crear `.github/workflows/ci.yml` (ya generado) → commit + push.
- **Verificar:** en la pestaña **Actions** del repo, ver la corrida ejecutando
  `./mvnw -B test` en cada push y PR a `main`.
- **Definition of done:** corrida en verde (o roja arreglada — ver Lombok).
- **Extra:** activar *branch protection* en `main` para que un pipeline rojo bloquee
  el merge.
- **CV:** "Diseñé el pipeline de CI del proyecto desde cero (build + suite de tests
  en cada push/PR)."

## Día 2 — Quality gates: cobertura (JaCoCo) + análisis estático (Sonar)
- **Construir:**
  - Agregar el plugin **JaCoCo** al `pom.xml`; generar reporte con
    `./mvnw test jacoco:report`.
  - Análisis estático: SonarCloud es gratis **para repos públicos**. Como este repo
    es privado, dos opciones honestas: (a) hacerlo público (es un demo) y usar
    SonarCloud, o (b) levantar **SonarQube Community local con Docker** (gratis) y
    correr el scanner contra él.
  - Configurar un gate: fallar el build si la cobertura baja de un umbral (ej. 70%).
  - Actualizar `ci.yml`: pasar de `test` a `./mvnw -B verify` y sumar el step de
    análisis.
- **Definition of done:** pipeline que rompe si no se cumple el gate.
- **CV:** "Implementé quality gates (cobertura JaCoCo + análisis estático) en el
  pipeline, bloqueando merges bajo el umbral."

## Día 3 — API testing con REST Assured sobre `/playlistdata`
- **Construir:**
  - Agregar dependencia `rest-assured` (scope `test`) al `pom.xml`.
  - Test de integración con `@SpringBootTest(webEnvironment = RANDOM_PORT)` y
    **`SpotifyService` mockeado** (`@MockBean`), para que REST Assured pegue al
    controller real pero sin depender del token de Spotify (que rota en cada uso).
  - Validar: status 200, estructura del JSON (`id`, `name`, `tracks.total`),
    y el caso 401 sin/ con Bearer inválido.
  - Integrar la suite al pipeline.
- **Definition of done:** API tests corriendo en CI.
- **CV:** "Automaticé pruebas de integración de API REST con REST Assured,
  integradas al pipeline de CI."

## Día 4 — Performance testing con JMeter sobre `/playlistdata`
- **Construir:**
  - Plan `.jmx`: Thread Group (ej. 50 usuarios, ramp-up 10s), HTTP Sampler a
    `127.0.0.1:8080/playlistdata` con header `Authorization: Bearer`, assertions de
    response code (200) y de tiempo de respuesta.
  - Para no depender de Spotify real: correr la app con `SpotifyService` mockeado
    (perfil de test) o un stub, y apuntar JMeter ahí.
  - Correr headless y generar reporte HTML:
    `jmeter -n -t plan.jmx -l results.jtl -e -o report/`.
- **Definition of done:** un `.jmx` versionado + reporte con latencia y throughput.
- **CV:** "Pruebas de performance (carga/estrés) con JMeter sobre APIs REST."
- **Alternativa moderna a evaluar luego:** k6.

## Día 5 — E2E con Playwright
- Este repo ya usa Microsoft Playwright para algunos E2E.
- **Ojo con OAuth:** el flujo `/login → /callback` necesita un login real de Spotify
  en navegador, difícil de automatizar de punta a punta. Enfocar Playwright en lo
  automatizable: que Swagger UI cargue, que los endpoints públicos respondan, y las
  validaciones de la capa web.
- **Mejor target si aplica:** si tengo un front **Angular** que consume este backend,
  ese es el lugar ideal para E2E de flujos de usuario con Playwright.
- **CV:** "Automatización E2E con Playwright ejecutada en CI."

## Día 6 — Reutilización de pipelines (salto a "arquitectura")
- **Construir:** refactorizar el workflow para extraer un **reusable workflow** o un
  **composite action** (ej. el job de quality gate), y consumirlo desde el pipeline
  principal. Este es el equivalente en GitHub Actions a los **CI/CD Components** de
  GitLab y a las **Shared Libraries** de Jenkins — justo el patrón centralizado que
  quiero entender de acidlabs.
- **CV:** "Diseñé workflows reutilizables (composite actions / reusable workflows)
  para estandarizar validaciones entre proyectos."

## Día 7 — Consolidación
- Recoger **métricas** de la semana: % de cobertura, nº de tests automatizados,
  latencia/throughput medidos con JMeter.
- Actualizar el CV maestro (SDET) con las líneas nuevas + métricas reales.
- Repaso teórico del syllabus de **ISTQB Foundation Level**; decidir si rendirlo.
- Preparar la narrativa de entrevista: poder explicar en voz alta qué construí y por
  qué (triggers, quality gates, por qué mockeo el servicio para los tests, etc.).

---

## Prioridad si la semana se complica
**Día 1 y 2 (CI + quality gates) > Día 3 (REST Assured) > Día 6 (reutilización) >
Día 4 (JMeter) > Día 5 (Playwright).** Los dos primeros son innegociables — son los
de mayor peso salarial y los que cierran mi brecha declarada de "crear pipelines".

## Resultado esperado al final de la semana
- Repo con: pipeline CI, quality gates (cobertura + análisis estático), API tests
  (REST Assured) y test de carga (JMeter), más un workflow reutilizable.
- CV maestro actualizado con líneas nuevas y métricas verificables.
- Poder decir, con honestidad, "esto lo construí yo" en cualquier entrevista.
