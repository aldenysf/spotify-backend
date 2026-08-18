package com.worldWide.spotify.spotifybackend;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import com.worldWide.spotify.spotifybackend.service.SpotifyAuthService;
import com.worldWide.spotify.spotifybackend.web.fixtures.PlaywrightTestCase;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SpotifyPlaylistTest extends PlaywrightTestCase {

	@LocalServerPort
	private int port;

	@Mock
	private SpotifyProperties spotifyProperties;

	@Mock
	private HttpSession session;

	private SpotifyAuthService spotifyAuthService;

	private String accessToken;
	private String baseUrl;

	@BeforeEach
	void setUp(){
		// Crear mocks
		lenient().when(spotifyProperties.getClientId()).thenReturn("5774fb96e96741c0b2d984b22ac28b99");
		lenient().when(spotifyProperties.getCurrentRefreshToken()).thenReturn("AQDeQMBFAOWJyWCIwQk0lVMrqPEH7Vto32Oe21aECOhuzr5wRaxSt31YXEjpGgghPI2HQMPEnUTnUQ15Ui4RsNVZN7O_qPx2tWFkMX1ZDFPMFdt7u7dC6cjw3qoYFGC_kXQ");
		RestTemplateBuilder builder = new RestTemplateBuilder();
		spotifyAuthService = new SpotifyAuthService(spotifyProperties,session,builder);
		accessToken = spotifyAuthService.getAccessTokenForTests();
		baseUrl = "http://127.0.0.1:" + port;
		System.out.println("Port: " + port);
	}

	@Test
	@DisplayName("Get Playlist metadata")
	void getPlaylist() {
		String playlistEndpoint = baseUrl + "/playlistdata";

		APIResponse response = page.request().get(
				playlistEndpoint,
				RequestOptions.create()
						.setHeader("Authorization", "Bearer " + accessToken)
						.setHeader("Accept", "application/json")
		);

		int status = response.status();
		String body = response.text();

		System.out.println("Status: " + status);
		System.out.println("Response: " + body);

		Assertions.assertEquals(200, status, "Playlist request failed: " + body);
	}

}
