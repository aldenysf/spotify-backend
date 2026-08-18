package com.worldWide.spotify.spotifybackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import com.worldWide.spotify.spotifybackend.service.SpotifyService;
import com.worldWide.spotify.spotifybackend.web.controller.PlaylistController;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlaylistController.class)
@AutoConfigureMockMvc(addFilters = false)
class SpotifyPlaylistTest {

	private static final String PLAYLIST_ID = "7xBLtlRNkinBdkNYy0BafL";

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private SpotifyService spotifyService;

	@MockBean
	private SpotifyProperties spotifyProperties;

	@Test
	@DisplayName("GET /playlistdata returns playlist metadata for a valid Bearer token")
	void getPlaylist() throws Exception {
		when(spotifyProperties.getPlaylistId()).thenReturn(PLAYLIST_ID);

		SpotifyPlaylistData playlist = new ObjectMapper().readValue(
				"{\"id\":\"" + PLAYLIST_ID + "\",\"name\":\"Worldwide Side by Side\"}",
				SpotifyPlaylistData.class);

		when(spotifyService.getPlaylist(PLAYLIST_ID, "fake-token")).thenReturn(playlist);

		mockMvc.perform(get("/playlistdata").header("Authorization", "Bearer fake-token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(PLAYLIST_ID))
				.andExpect(jsonPath("$.name").value("Worldwide Side by Side"));
	}

	@Test
	@DisplayName("GET /playlistdata without Authorization header fails")
	void getPlaylistWithoutAuthHeader() {
		ServletException thrown = assertThrows(ServletException.class,
				() -> mockMvc.perform(get("/playlistdata")));

		assertInstanceOf(IllegalStateException.class, thrown.getCause());
	}
}
