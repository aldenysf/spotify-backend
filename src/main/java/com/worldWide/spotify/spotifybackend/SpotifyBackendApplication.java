package com.worldWide.spotify.spotifybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class SpotifyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(	SpotifyBackendApplication.class, args);
	}

}
