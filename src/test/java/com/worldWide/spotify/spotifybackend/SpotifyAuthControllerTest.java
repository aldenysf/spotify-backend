package com.worldWide.spotify.spotifybackend;


import com.microsoft.playwright.Route;
import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.service.SpotifyAuthService;
import com.worldWide.spotify.spotifybackend.web.fixtures.PlaywrightTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SpotifyAuthControllerTest extends PlaywrightTestCase {

    @Mock
    private SpotifyProperties spotifyProperties;

    @Mock
    private SpotifyAuthService spotifyAuthService;

    @BeforeEach
    void setUp() {
        // Crear mocks
        spotifyAuthService = mock(SpotifyAuthService.class);
        spotifyProperties = mock(SpotifyProperties.class);

        // Mock del redirect URI
        lenient().when(spotifyProperties.getRedirectUri()).thenReturn("http://127.0.0.1:8080/callback");

        // Mock de intercambio code → token
        when(spotifyAuthService.exchangeCodeForToken("mockCode123", "mockVerifier"))
                .thenReturn(Map.of(
                        "access_token", "mockAccessToken",
                        "refresh_token", "mockRefreshToken"
                ));
    }

    @Test
    void shouldRedirectToCallbackWithMockedAuthCode() {
        // 1. Interceptar TODAS las requests
        page.route("**/authorize**", route -> {
            route.fulfill(new Route.FulfillOptions()
                    .setStatus(302)
                    .setHeaders(Map.of(
                            "Location", "http://127.0.0.1:8080/callback?code=mockCode123&state=mockState456"
                    ))
            );
        });

        // 2. Navegar a /login → debería simular el flujo entero
        page.navigate("http://127.0.0.1:8080/login");


        // 3. Esperar al callback
        page.waitForURL("**/callback*");
        String currentUrl = page.url();
        System.out.println("Redirected to: " + currentUrl);

        // 4. Validar parámetros en la URL
        assertTrue(currentUrl.contains("code=mockCode123"), "URL should contain authorization code");
        assertTrue(currentUrl.contains("state=mockState456"), "URL should contain state parameter");

        // 5. Validar que se guardaron los tokens (texto en tu UI simulado)
        assertTrue(page.getByText("Tokens obtenidos y guardados correctamente").isVisible());
    }
}
