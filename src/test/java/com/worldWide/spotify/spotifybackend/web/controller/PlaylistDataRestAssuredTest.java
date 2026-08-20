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
                "spotify.current-refresh-token=test-refresh-token",
                "server.error.include-message=always"
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
    void getPlaylistData_withoutAuthHeader_returns500WithErrorBody() {
        // SecurityConfig permite /error (permitAll), así que el forward interno de
        // Tomcat tras el IllegalStateException llega hasta BasicErrorController y
        // arma un 500 real con body JSON, en vez de que Spring Security lo corte
        // antes con un 403 vacío.
        given()
        .when()
                .get("/playlistdata")
        .then()
                .statusCode(500)
                .body("status", equalTo(500))
                .body("error", equalTo("Internal Server Error"))
                .body("message", equalTo("Missing or invalid Authorization header"))
                .body("path", equalTo("/playlistdata"));
    }
}
