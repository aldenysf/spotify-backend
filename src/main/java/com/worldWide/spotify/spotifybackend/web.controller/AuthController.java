package com.worldWide.spotify.spotifybackend.web.controller;

import com.worldWide.spotify.spotifybackend.service.SpotifyAuthService;
import com.worldWide.spotify.spotifybackend.util.PKCEUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.UUID;

@Controller
public class AuthController {

    @Autowired
    private final SpotifyAuthService spotifyAuthService;


    public AuthController(SpotifyAuthService spotifyAuthService) {
        this.spotifyAuthService = spotifyAuthService;
    }

    @GetMapping("/login")
    public RedirectView login(HttpSession session) throws Exception {
        String codeVerifier = PKCEUtil.generateCodeVerifier();
        String codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier);
        String state = UUID.randomUUID().toString();
        //String state = "mockState456";

        session.setAttribute("code_verifier", codeVerifier);
        session.setAttribute("state", state);

        String authUrl = spotifyAuthService.buildSpotifyAuthUrl(codeChallenge, state);
        return new RedirectView(authUrl);
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code,
                                           @RequestParam String state,
                                           HttpSession session) {
        String expectedState = (String) session.getAttribute("state");
        String codeVerifier = (String) session.getAttribute("code_verifier");
        System.out.println("expectedState " + expectedState );
        System.out.println("codeverifier " + codeVerifier );
        System.out.println("state " +state );

        if (!state.equals(expectedState)) {
            return ResponseEntity.badRequest().body("Error: invalid state");
        }

        try {
            Map<String, Object> tokenData = spotifyAuthService.exchangeCodeForToken(code, codeVerifier);

                String accessToken = (String) tokenData.get("access_token");
                String refreshToken = (String) tokenData.get("refresh_token");

                session.setAttribute("access_token", accessToken);
                session.setAttribute("refresh_token", refreshToken);

                return ResponseEntity.ok("Tokens obtenidos y guardados correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en token exchange: " + e.getMessage());
        }
    }
}
