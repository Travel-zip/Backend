package com.example.demo.map.api.dto;

import lombok.Data;


// [SEARCH 기능]
//  /api/search 응답 전용 DTO (overview 포함)
@Data
public class PlaceSearchResultDto {
    private String title;
    private String addr1;
    private Double mapx;
    private Double mapy;
    private String firstimage;
    private String contentid;
    private String contenttypeid;
    private Double dist;

    private String overview; // 소개글
}