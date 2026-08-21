package com.worldWide.spotify.spotifybackend.config;

import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import com.worldWide.spotify.spotifybackend.service.SpotifyAuthService;
import com.worldWide.spotify.spotifybackend.service.SpotifyService;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Reemplaza a {@link SpotifyService} cuando el profile "perf" está activo, para
 * que las pruebas de carga con JMeter midan el rendimiento real de esta app
 * (Controller + Security + Tomcat) sin depender de la API real de Spotify.
 */
@Service
@Profile("perf")
@Primary
public class PerfStubSpotifyService extends SpotifyService {

    public PerfStubSpotifyService(RestTemplateBuilder restTemplateBuilder,
                                   SpotifyProperties spotifyProperties,
                                   SpotifyAuthService spotifyAuthService) {
        super(restTemplateBuilder, spotifyProperties, spotifyAuthService);
    }

    @Override
    public SpotifyPlaylistData getPlaylist(String playlistId, String token) {
        SpotifyPlaylistData.Owner owner = new SpotifyPlaylistData.Owner();
        owner.setId("perf-owner");
        owner.setDisplayName("Perf Test");

        SpotifyPlaylistData.Tracks tracks = new SpotifyPlaylistData.Tracks();
        tracks.setTotal(4);

        SpotifyPlaylistData playlist = new SpotifyPlaylistData();
        playlist.setId(playlistId);
        playlist.setName("Perf Test Playlist");
        playlist.setDescription("Stub data for JMeter load testing");
        playlist.setOwner(owner);
        playlist.setTracks(tracks);

        return playlist;
    }
}
