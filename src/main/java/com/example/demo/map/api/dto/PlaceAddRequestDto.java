package com.example.demo.map.api.dto;

import lombok.Data;

// [추가] 프론트엔드에서 수동으로 장소를 추가할 때 사용할 DTO (imageUrl 제외)
@Data
public class PlaceAddRequestDto {
    private String roomId;
    private String title;
    private Double lat;
    private Double lng;
}
