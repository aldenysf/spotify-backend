package com.worldWide.spotify.spotifybackend.web.services;

import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.data.SpotifyTokenResponseData;
import com.worldWide.spotify.spotifybackend.service.SpotifyAuthService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SpotifyAuthServiceTest {

    public static final String INVALID_CODE_VERIFIER_ERROR_MESSAGE = "Code verifier cannot be null or empty";
    public static final String INVALID_AUTHORIZATION_CODE_ERROR_MESSAGE = "Authorization code cannot be null or empty";
    public static final String INVALID_REFRESH_TOKEN_ERROR_MESSAGE = "No refresh token found, Log in again to get it.";

    @Mock
    private SpotifyProperties spotifyProperties;

    @Mock
    private HttpSession session;

    @Mock
    private RestTemplate restTemplate;

    private SpotifyAuthService spotifyAuthService;

    @BeforeEach
    void setUp() {
        when(spotifyProperties.getClientId()).thenReturn("test-client-id");
        when(spotifyProperties.getRedirectUri()).thenReturn("http://127.0.0.1:8080/callback");
        lenient().when(spotifyProperties.getScopes()).thenReturn(List.of("playlist-modify-private","playlist-modify-public","playlist-read-private"));

        RestTemplateBuilder builder = mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);

        spotifyAuthService = new SpotifyAuthService(spotifyProperties, session, builder);
    }

    @Test
    void testBuildSpotifyAuthUrl() {
        String url = spotifyAuthService.buildSpotifyAuthUrl("challenge123", "state123");
        System.out.println(url);
        assertTrue(url.contains("client_id=test-client-id"));
        assertTrue(url.contains("redirect_uri=http://127.0.0.1:8080/callback"));
        assertTrue(url.contains("scope=playlist-modify-private%20playlist-modify-public%20playlist-read-private"));
        assertTrue(url.contains("code_challenge=challenge123"));
        assertTrue(url.contains("state=state123"));
    }

    @Test
    void testExchangeCodeForTokenBodyCreation_Success() {
        MultiValueMap<String, String> body = spotifyAuthService.exchangeCodeForTokenBodyCreation("authCode", "verifier123");

        Assertions.assertEquals("authCode", body.getFirst("code"));
        assertEquals("verifier123", body.getFirst("code_verifier"));
        assertEquals("test-client-id", body.getFirst("client_id"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testExchangeCodeForTokenBodyCreation_InvalidCode_ThrowsException(String invalidCode) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> spotifyAuthService.exchangeCodeForTokenBodyCreation(invalidCode, "verifier123")
        );

        assertEquals(INVALID_AUTHORIZATION_CODE_ERROR_MESSAGE, exception.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void testExchangeCodeForTokenBodyCreation_InvalidCodeVerifier_ThrowsException(String invalidVerifier) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> spotifyAuthService.exchangeCodeForTokenBodyCreation("authCode123", invalidVerifier)
        );

        assertEquals(INVALID_CODE_VERIFIER_ERROR_MESSAGE, exception.getMessage());
    }

    @Test
    void testSetAndGetAccessToken() {
        spotifyAuthService.setAccessToken("token123");

        verify(session).setAttribute("access_token", "token123");
    }

    @Test
    void testClearAccessToken() {
        when(session.getAttribute("access_token")).thenReturn("some-token");

        spotifyAuthService.clearAccessToken();

        verify(session).removeAttribute("access_token");

        when(session.getAttribute("access_token")).thenReturn(null);
        assertNull(session.getAttribute("access_token"));
    }

    @Test
    void testRefreshAccessToken_Success() {
        // Simulamos refresh token en sesión
        when(session.getAttribute("refresh_token")).thenReturn("refresh123");

        // Mockeamos respuesta de Spotify
        SpotifyTokenResponseData mockResponse = new SpotifyTokenResponseData();
        mockResponse.setAccessToken("newAccessToken");
        mockResponse.setRefreshToken("newRefreshToken");

        ResponseEntity<SpotifyTokenResponseData> responseEntity =
                new ResponseEntity<>(mockResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(SpotifyTokenResponseData.class)))
                .thenReturn(responseEntity);

        String token = spotifyAuthService.refreshAccessToken();

        assertEquals("newAccessToken", token);
        verify(session).setAttribute("access_token", "newAccessToken");
        verify(session).setAttribute("refresh_token", "newRefreshToken");
    }

    @Test
    void testRefreshAccessToken_ShouldThrowIfNoRefreshToken() {
        when(session.getAttribute("refresh_token")).thenReturn(null);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> spotifyAuthService.refreshAccessToken()
        );

        assertEquals(INVALID_REFRESH_TOKEN_ERROR_MESSAGE, exception.getMessage());
    }
}
