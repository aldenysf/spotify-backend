package com.worldWide.spotify.spotifybackend.data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SpotifyPlaylistData {

    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;

    @JsonProperty("collaborative")
    private boolean collaborative;

    @JsonProperty("external_urls")
    private Map<String, String> externalUrls;

    @JsonProperty("owner")
    private Owner owner;

    @JsonProperty("tracks")
    private Tracks tracks;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Owner {
        @JsonProperty("id")
        private String id;

        @JsonProperty("display_name")
        private String displayName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Tracks {
        @JsonProperty("total")
        private int total;
    }
}
