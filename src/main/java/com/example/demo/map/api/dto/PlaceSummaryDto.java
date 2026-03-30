package com.example.demo.map.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// [NEARBY 기능]
@Data
@AllArgsConstructor
public class PlaceSummaryDto {
    private String title;
    private Double lat;      // mapy
    private Double lng;      // mapx
    private String category; // attraction/culture/...

    private String contentId; // TourAPI 콘텐츠ID(상세조회 정확도용)

    private String imageUrl; // [사진추가]
}
