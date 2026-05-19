package com.example.demo.map.api;

import com.example.demo.auth.api.ApiException;
import com.example.demo.chat.application.RoomService;
import com.example.demo.map.api.dto.*;
import com.example.demo.map.application.TourApiService;
import com.example.demo.map.application.TourPlaceService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

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

    // =========================================================
    // [추가] 프론트에서 특정 장소를 수동으로 tour_places에 추가
    // =========================================================
    @PostMapping("/api/places")
    public ResponseEntity<?> addCustomPlace(@RequestBody PlaceAddRequestDto request) {
        // 필수 파라미터 검증
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "장소 이름(title)은 필수입니다.");
        }
        if (request.getLat() == null || request.getLng() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "위도(lat)와 경도(lng)는 필수입니다.");
        }

        // Service 호출하여 DB에 저장
        tourPlaceService.addManualPlace(request);

        // 프론트엔드가 처리하기 쉽게 간단한 성공 JSON 응답 반환
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "장소가 성공적으로 추가되었습니다."
        ));
    }

    // =========================================================
    // [추가] 프론트에서 여러 장소를 한 번에 tour_places에 일괄 추가
    // URL 구분을 위해 /bulk 를 붙여줍니다.
    // =========================================================
    @PostMapping("/api/places/bulk")
    public ResponseEntity<?> addCustomPlacesBulk(@RequestBody PlaceAddListRequestDto request) {
        // 파라미터 검증
        if (request.getPlaces() == null || request.getPlaces().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "추가할 장소 목록(places)이 비어있습니다.");
        }

        // Service 호출하여 DB에 일괄 저장
        tourPlaceService.addManualPlaces(request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", request.getPlaces().size() + "개의 장소가 성공적으로 확인/추가 되었습니다."
        ));
    }




}
