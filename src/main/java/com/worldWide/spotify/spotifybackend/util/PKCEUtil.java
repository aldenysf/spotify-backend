package com.worldWide.spotify.spotifybackend.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;

public class PKCEUtil {

    public static String generateCodeVerifier() {
        SecureRandom sr = new SecureRandom();
        byte[] code = new byte[64];
        sr.nextBytes(code);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(code);
    }

    public static String generateCodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(codeVerifier.getBytes("US-ASCII"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}

