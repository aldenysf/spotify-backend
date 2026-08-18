package com.worldWide.spotify.spotifybackend.service;

import com.worldWide.spotify.spotifybackend.config.SpotifyProperties;
import com.worldWide.spotify.spotifybackend.data.SpotifyTokenResponseData;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;


@Service
public class SpotifyAuthService {

    @Autowired
    private final SpotifyProperties spotifyProperties;
    private final HttpSession session;
    private final RestTemplate restTemplate;

    public SpotifyAuthService(SpotifyProperties spotifyProperties, HttpSession session, RestTemplateBuilder restTemplateBuilder) {
        this.spotifyProperties = spotifyProperties;
        this.session = session;
        this.restTemplate = restTemplateBuilder.build();
    }

    @PostConstruct
    public void init() {
        System.out.println("Client ID: " + spotifyProperties.getClientId());
    }


    public String buildSpotifyAuthUrl(String codeChallenge, String state) {

        System.out.println("Code challenge " + codeChallenge);
        System.out.println("State " + state);

        return UriComponentsBuilder.fromHttpUrl("https://accounts.spotify.com/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", spotifyProperties.getClientId())
                .queryParam("redirect_uri", spotifyProperties.getRedirectUri())
                .queryParam("scope", String.join(" ", spotifyProperties.getScopes()))
                .queryParam("code_challenge_method", "S256")
                .queryParam("code_challenge", codeChallenge)
                .queryParam("state", state)
                .toUriString();
    }

    public MultiValueMap<String, String> exchangeCodeForTokenBodyCreation(String code, String codeVerifier){

        if (Objects.isNull(code) || StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty");
        }

        if (Objects.isNull(codeVerifier) || StringUtils.isBlank(codeVerifier)) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", spotifyProperties.getRedirectUri());
        body.add("client_id", spotifyProperties.getClientId());
        body.add("code_verifier", codeVerifier);

        return body;
    }

    public Map<String, Object> exchangeCodeForToken(String code, String codeVerifier) {
        MultiValueMap<String, String> body = exchangeCodeForTokenBodyCreation(code, codeVerifier);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.postForEntity("https://accounts.spotify.com/api/token", request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Error al obtener token desde Spotify. Status: " + response.getStatusCode());
        }

        Map<String,Object> tokens = response.getBody();

        if(tokens.containsKey("refresh_token")){
            String refreshToken = (String) tokens.get("refresh_token");
            session.setAttribute("refresh_token", refreshToken);
            System.out.println("Nuevo refresh token obtenido: " + refreshToken);
            System.out.println("Nuevo access token obtenido: " + tokens.get("access_token"));
        }
        session.setAttribute("access_token", tokens.get("access_token"));
        return tokens;
    }

    public String getAccessToken(){
        Object token = session.getAttribute("access_token");
                return token != null ? token.toString() : null;
    }

    public void setAccessToken(String accessToken){
        session.setAttribute("access_token",accessToken);
    }

    public void clearAccessToken(){
        session.removeAttribute("access_token");
    }

    public String refreshAccessToken() {
        String refreshToken = StringUtils.defaultIfBlank(spotifyProperties.getCurrentRefreshToken(),
                (String) session.getAttribute("refresh_token"));


        if (Objects.isNull(refreshToken) || refreshToken.isBlank()) {
            throw new IllegalStateException("No refresh token found, Log in again to get it.");
        }
        // Body
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type","refresh_token");
        body.add("refresh_token",refreshToken);
        body.add("client_id", spotifyProperties.getClientId());

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // RequestEntity
        HttpEntity<MultiValueMap<String,String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<SpotifyTokenResponseData> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token",
                request,
                SpotifyTokenResponseData.class
        );

        if (response.getStatusCode().is2xxSuccessful() && Objects.nonNull(response.getBody())) {
            String newAccessToken = response.getBody().getAccessToken();
            String newRefreshToken = response.getBody().getRefreshToken();

            session.setAttribute("access_token", newAccessToken);
            session.setAttribute("refresh_token", newRefreshToken);

            return newAccessToken;
        } else {
            throw new RuntimeException("Failed to refresh token from Spotify");
        }
    }

    public String getAccessTokenForTests() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", spotifyProperties.getCurrentRefreshToken());
        body.add("client_id", spotifyProperties.getClientId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://accounts.spotify.com/api/token", request, Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        } else {
            throw new RuntimeException("Failed to refresh Spotify access token");
        }
    }

}
