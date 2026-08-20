package com.worldWide.spotify.spotifybackend.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import com.worldWide.spotify.spotifybackend.service.SpotifyService;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spotify.client-secret=test-secret",
                "spotify.current-refresh-token=test-refresh-token"
        }
)
class PlaylistDataRestAssuredTest {

    private static final String PLAYLIST_ID = "7xBLtlRNkinBdkNYy0BafL";

    @LocalServerPort
    private int port;

    @MockBean
    private SpotifyService spotifyService;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void getPlaylistData_withValidBearer_returns200AndJsonShape() throws Exception {
        SpotifyPlaylistData playlist = new ObjectMapper().readValue(
                "{\"id\":\"" + PLAYLIST_ID + "\",\"name\":\"Worldwide Side by Side\","
                        + "\"tracks\":{\"total\":42}}",
                SpotifyPlaylistData.class);

        when(spotifyService.getPlaylist(anyString(), eq("fake-token"))).thenReturn(playlist);

        given()
                .header("Authorization", "Bearer fake-token")
        .when()
                .get("/playlistdata")
        .then()
                .statusCode(200)
                .body("id", equalTo(PLAYLIST_ID))
                .body("name", equalTo("Worldwide Side by Side"))
                .body("tracks.total", equalTo(42));
    }

    @Test
    void getPlaylistData_withoutAuthHeader_returns403() {
        // Verificado empíricamente contra Tomcat real: el controller lanza
        // IllegalStateException, Tomcat reenvía a /error, pero SecurityConfig no
        // tiene permitAll("/error"), así que Http403ForbiddenEntryPoint responde
        // 403 antes de que Spring Boot arme un 500. Distinto del slice test
        // @WebMvcTest (SpotifyPlaylistTest), que desactiva los filtros de
        // seguridad y ve la excepción cruda en vez de un status HTTP.
        given()
        .when()
                .get("/playlistdata")
        .then()
                .statusCode(403);
    }
}
