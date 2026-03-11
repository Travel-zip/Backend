package com.example.demo.map.api.dto;

import java.util.Arrays;
import java.util.List;


// [NEARBY 기능]
public enum TourCategory {
    ATTRACTION("attraction", "12"), // 관광지
    CULTURE("culture", "14"),       // 문화시설
    FESTIVAL("festival", "15"),     // 행사/공연/축제
    LEPORTS("leports", "28"),       // 레포츠
    STAY("stay", "32"),             // 숙박
    SHOPPING("shopping", "38"),     // 쇼핑
    FOOD("food", "39");             // 음식점

    private final String key;
    private final String contentTypeId;

    TourCategory(String key, String contentTypeId) {
        this.key = key;
        this.contentTypeId = contentTypeId;
    }

    public String key() { return key; }
    public String contentTypeId() { return contentTypeId; }

    public static List<TourCategory> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(TourCategory::fromKey)
                .toList();
    }

    public static TourCategory fromKey(String key) {
        for (TourCategory c : values()) {
            if (c.key.equalsIgnoreCase(key)) return c;
        }
        return ATTRACTION;
    }
}
