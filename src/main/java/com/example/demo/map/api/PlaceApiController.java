package com.example.demo.map.api;

import com.example.demo.auth.api.ApiException;
import com.example.demo.chat.application.RoomService;
import com.example.demo.map.api.dto.*;
import com.example.demo.map.application.TourApiService;
import com.example.demo.map.application.TourPlaceService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

// 기존 AttractionController 역할 확장(관광/맛집/숙박 + 시군구)
@RestController
@RequiredArgsConstructor
public class PlaceApiController {

    private final TourApiService tourApiService;

    private final RoomService roomService;
    private final TourPlaceService tourPlaceService;





    // =========================================================
    // [SEARCH 기능] /api/search
    // - 카테고리 + 키워드 + (lat,lng,radius) 기반으로 검색
    // - 결과에 overview(소개글) 포함
    // =========================================================

    @GetMapping("/api/search")
    public List<PlaceSearchResultDto> search(@RequestParam String category,
                                             @RequestParam String keyword,
                                             //  전국 검색 방지: 중심좌표/반경 필수
                                             @RequestParam double lat,
                                             @RequestParam double lng,
                                             @RequestParam int radius,
                                             @RequestParam(required = false) String roomId

                                             // @RequestParam(required = false) String areaCode,
                                             //  @RequestParam(required = false) String sigunguCode
                                         ) {

        // [수정] 검색 반경이 5km(5000m)로 확장되었으므로 검증 허용치도 5000으로 늘려줍니다.
        if (radius < 50 || radius > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "radius는 50~5000(m) 범위로 입력하세요."); // [수정] 에러 메시지 변경
        }

        String rid = (roomId == null || roomId.isBlank()) ? "default" : roomId;
        roomService.getOrCreate(rid);



        //  category를 "숫자 contentTypeId" 또는 "키" 둘 다 허용
        String cat = (category == null) ? "" : category.trim();

        String contentTypeId;

        //  12/14/15/28/32/38/39 처럼 숫자로 들어오면 그대로 사용
        if (cat.matches("\\d+")) {
            contentTypeId = cat;
        } else {
            // 기존 호환: restaurant -> food(39)
            String key = cat.toLowerCase();
            if ("restaurant".equals(key)) key = "food";

            //  TourCategory로 매핑 (없는 키면 ATTRACTION 기본값)
            contentTypeId = TourCategory.fromKey(key).contentTypeId();
        }


        List<PlaceSearchResultDto> results =
                tourApiService.searchByKeywordWithinRadiusWithOverview(lat, lng, radius, contentTypeId, keyword);

        //  SEARCH 결과를 DB에 저장 (title/lat/lng만)
        tourPlaceService.saveSearchResults(rid, results);


        return results;

    }

    // =========================================================
    // [NEARBY 기능] /api/places/nearby
    // - 중심좌표(lat,lng) + 반경(radius) + categories(csv)
    // - 주변 장소 요약(title/lat/lng/category/contentId) 목록 반환
    // =========================================================
    @GetMapping("/api/places/nearby")
    public List<PlaceSummaryDto> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam int radius,
            @RequestParam String categories,
            @RequestParam(required = false) String roomId
    ) {
        // 값 검증(너무 큰 반경 방지)
        if (radius < 50 || radius > 2500) { // 필요하면 범위 조절
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "radius는 50~2500(m) 범위로 입력하세요.");
        }

        String rid = (roomId == null || roomId.isBlank()) ? "default" : roomId;
        roomService.getOrCreate(rid);

        var cats = TourCategory.parseCsv(categories);


        List<PlaceSummaryDto> results = tourApiService.getNearbySummaries(lat, lng, radius, cats);

        //  NEARBY 결과를 DB에 저장 (title/lat/lng만)
        tourPlaceService.saveNearbyResults(rid, results);


        return results;


    }




}
