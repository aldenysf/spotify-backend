package com.worldWide.spotify.spotifybackend.config;

import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import com.worldWide.spotify.spotifybackend.service.SpotifyAuthService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PerfStubSpotifyServiceTest {

    @Mock
    private SpotifyProperties spotifyProperties;

    @Mock
    private HttpSession session;

    @Test
    void getPlaylist_returnsCannedDataWithoutNetworkCalls() {
        SpotifyAuthService spotifyAuthService =
                new SpotifyAuthService(spotifyProperties, session, new RestTemplateBuilder());

        PerfStubSpotifyService stub =
                new PerfStubSpotifyService(new RestTemplateBuilder(), spotifyProperties, spotifyAuthService);

        SpotifyPlaylistData playlist = stub.getPlaylist("any-playlist-id", "any-token");

        assertEquals("any-playlist-id", playlist.getId());
        assertEquals("Perf Test Playlist", playlist.getName());
        assertEquals(4, playlist.getTracks().getTotal());
        assertEquals("perf-owner", playlist.getOwner().getId());
    }
}
