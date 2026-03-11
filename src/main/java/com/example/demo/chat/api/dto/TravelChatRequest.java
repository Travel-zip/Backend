package com.example.demo.chat.api.dto;

import lombok.Data;

@Data
public class TravelChatRequest {

    private String roomId;      // 예: jeju-trip-2025
    private String chatLog;     // 원본 채팅 로그 전체

    //  사용자가 리스트에서 선택한 관광지 정보
    private String selectedPlaceName;
    private Double selectedLat;
    private Double selectedLng;

    // 사용자가 리스트에서 선택한 맛집(음식점) 정보
    private String selectedRestaurantName;
    private Double selectedRestaurantLat;
    private Double selectedRestaurantLng;

    // 사용자가 리스트에서 선택한 숙박 정보
    private String selectedStayName;
    private Double selectedStayLat;
    private Double selectedStayLng;
}
