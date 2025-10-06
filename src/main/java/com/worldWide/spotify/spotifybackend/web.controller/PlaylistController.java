package com.worldWide.spotify.spotifybackend.web.controller;

import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import com.worldWide.spotify.spotifybackend.service.SpotifyService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Objects;

@Controller
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
        final String playlidtId = spotifyProperties.getPlaylistId();

        if(!Objects.nonNull(token) || !StringUtils.isBlank(playlidtId)){
            SpotifyPlaylistData playlist = spotifyService.getPlaylist(playlidtId);
            return  ResponseEntity.ok(playlist);

        }else {
            throw new IllegalStateException("Token request is null, please refresh token");
        }
    }

}
