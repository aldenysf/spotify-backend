package com.worldWide.spotify.spotifybackend.service;


import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.data.SpotifyPlaylistData;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class SpotifyService {

    private final RestTemplate restTemplate;
    private final SpotifyProperties spotifyProperties;
    private String spotifyApiBaseUrl;
    private final SpotifyAuthService spotifyAuthService;

        public SpotifyService(RestTemplateBuilder restTemplateBuilder, SpotifyProperties spotifyProperties,SpotifyAuthService spotifyAuthService) {
        this.restTemplate = restTemplateBuilder.build();
        this.spotifyProperties = spotifyProperties;
        this.spotifyAuthService = spotifyAuthService;
    }

    public SpotifyPlaylistData getPlaylist(String playlistId) {
        String url = spotifyProperties.getBaseUrl() + "/playlists/" + playlistId;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(spotifyAuthService.getAccessToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<SpotifyPlaylistData> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    SpotifyPlaylistData.class
            );
            System.out.println("Response: " + response.getBody());
            return response.getBody();

        } catch (HttpClientErrorException.Unauthorized ex) {
            spotifyAuthService.refreshAccessToken();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(spotifyAuthService.getAccessToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<SpotifyPlaylistData> retryResponse = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    SpotifyPlaylistData.class
            );

            return retryResponse.getBody();

        } catch (HttpClientErrorException ex) {
            throw new RuntimeException("Error al obtener playlist: " + ex.getStatusCode(), ex);
        }
    }




}
