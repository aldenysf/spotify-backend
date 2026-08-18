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

}
