package com.example.demo.map.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourDetailCommonDto {
    private Response response;

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response { private Body body; }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body { private Items items; }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JsonProperty("item")
        private List<Item> item;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String title;
        private String overview;
        private String addr1;
        private Double mapx;
        private Double mapy;
        private String firstimage;
    }
}