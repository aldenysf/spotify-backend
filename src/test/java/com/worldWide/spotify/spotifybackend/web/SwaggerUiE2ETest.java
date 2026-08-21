package com.worldWide.spotify.spotifybackend.web;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E real con Playwright, acotado a lo que de verdad se puede automatizar sin
 * un login interactivo de Spotify: que la documentación pública de la API
 * (Swagger UI) cargue en un navegador real y liste el endpoint /playlistdata.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spotify.client-secret=test-secret",
                "spotify.current-refresh-token=test-refresh-token"
        }
)
class SwaggerUiE2ETest {

    @LocalServerPort
    private int port;

    private static Playwright playwright;
    private static Browser browser;
    private Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        browser.close();
        playwright.close();
    }

    @BeforeEach
    void newPage() {
        page = browser.newPage();
    }

    @AfterEach
    void closePage() {
        page.close();
    }

    @Test
    void swaggerUi_loadsAndListsPlaylistdataEndpoint() {
        page.navigate("http://127.0.0.1:" + port + "/swagger-ui/index.html");

        page.waitForSelector(".swagger-ui .opblock-summary-path");

        assertTrue(page.title().contains("Swagger UI"));
        assertTrue(page.content().contains("/playlistdata"));
    }
}
