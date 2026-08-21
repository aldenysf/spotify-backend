package com.worldWide.spotify.spotifybackend.web.controller;

import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import com.worldWide.spotify.spotifybackend.service.SpotifyService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class PlaylistController {
    @Autowired
    private SpotifyService spotifyService;
    private SpotifyProperties spotifyProperties;

    public PlaylistController(SpotifyProperties spotifyProperties) {
        this.spotifyProperties = spotifyProperties;
    }

    @GetMapping("/playlist")
    public ResponseEntity<SpotifyPlaylistData> playlist(HttpSession session){
        Object token = session.getAttribute("access_token").toString();
        final String playlistId = spotifyProperties.getPlaylistId();

        if(!Objects.nonNull(token) || !StringUtils.isBlank(playlistId)){
            SpotifyPlaylistData playlist = spotifyService.getPlaylist(playlistId);
            return  ResponseEntity.ok(playlist);

        }else {
            throw new IllegalStateException("Token request is null, please refresh token");
        }
    }

    @GetMapping("/playlistdata")
    public ResponseEntity<SpotifyPlaylistData> playlist(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        final String playlistId = spotifyProperties.getPlaylistId();

        SpotifyPlaylistData playlist = spotifyService.getPlaylist(playlistId, token);
        return ResponseEntity.ok(playlist);
    }

}
