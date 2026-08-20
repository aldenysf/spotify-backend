package com.worldWide.spotify.spotifybackend.web.controller;

import com.worldWide.spotify.spotifybackend.service.SpotifyAuthService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final String MOCK_AUTH_URL = "https://accounts.spotify.com/authorize?mock=1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpotifyAuthService spotifyAuthService;

    @Test
    void login_redirectsToSpotifyAuthUrl_andStoresPkceStateInSession() throws Exception {
        when(spotifyAuthService.buildSpotifyAuthUrl(anyString(), anyString()))
                .thenReturn(MOCK_AUTH_URL);

        MvcResult result = mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(MOCK_AUTH_URL))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertNotNull(session, "El controller debe crear una sesión");
        assertNotNull(session.getAttribute("code_verifier"), "code_verifier debe quedar guardado en sesión");
        assertNotNull(session.getAttribute("state"), "state debe quedar guardado en sesión");
    }
}
