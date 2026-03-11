package com.example.demo.map.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

// 한국관광공사 TourAPI 응답을 매핑하기 위한 전체 DTO
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourApiDto {
    private Response response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Body body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;
        private int totalCount;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Items {
        @JsonProperty("item")
        private List<Item> item;
    }

    // 실제 관광지 데이터 하나를 담는 클래스
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private String title;       // 관광지명
        private String addr1;       // 주소
        private Double mapx;        // 경도 (Longitude)
        private Double mapy;        // 위도 (Latitude)
        private String firstimage;  // 대표 이미지 URL
        private String contentid;   // 콘텐츠 ID

        private String contenttypeid; // contentTypeId (카테고리 구분)
        private Double dist;          // 거리(미터) - locationBasedList2에서 내려올 수 있음
    }
}
