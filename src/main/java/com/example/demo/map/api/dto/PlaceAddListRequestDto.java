package com.example.demo.map.api.dto;

import lombok.Data;

import java.util.List;

// [추가] 여러 장소를 한 번에 추가하기 위한 DTO
@Data
public class PlaceAddListRequestDto {
    private String roomId;
    private List<PlaceItem> places;

    // 내부 클래스로 개별 장소 데이터를 정의
    @Data
    public static class PlaceItem {
        private String title;
        private Double lat;
        private Double lng;
    }
}