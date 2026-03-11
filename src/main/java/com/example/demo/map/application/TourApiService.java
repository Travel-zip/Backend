package com.example.demo.map.application;

import com.example.demo.map.api.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
@RequiredArgsConstructor
public class TourApiService {

    // [추가] 운영 기본값 (필요시 조절)
    private static final int NEARBY_ROWS_PER_CATEGORY = 50; // [추가] 100 -> 50
    private static final int SEARCH_CANDIDATE_ROWS = 120;   // [추가] 200 -> 120
    // [추가] 외부 API 타임아웃 (서버 스레드 붙잡힘 방지)
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 7000;
    private static final Logger log = LoggerFactory.getLogger(TourApiService.class); // [추가]

    @Value("${tour.api.key}")
    private String serviceKey;

    // =========================================================
    // [NEARBY 기능] TourAPI locationBasedList2 URL
    // - 반경(radius) 기반으로 주변 목록을 가져오는 핵심 API
    // =========================================================
    private static final String LOCATION_URL =
            "https://apis.data.go.kr/B551011/KorService2/locationBasedList2";

    // =========================================================
    // [SEARCH 기능] 결과에 overview를 붙이기 위해 detailCommon2 사용
    // =========================================================
    private static final String DETAIL_COMMON_URL =
            "https://apis.data.go.kr/B551011/KorService2/detailCommon2";


    //private final RestTemplate restTemplate = new RestTemplate();
    // [수정] 타임아웃 있는 RestTemplate 사용
    private final RestTemplate restTemplate = createRestTemplate(); // [수정]

    private static RestTemplate createRestTemplate() { // [추가]
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(CONNECT_TIMEOUT_MS);
        f.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(f);
    }



    // =========================================================
    // [추가] 로그 폭탄 방지 (응답 바디 길이 제한)
    // =========================================================
    private String shorten(String s) { // [추가]
        if (s == null) return null;
        int max = 1000;
        return (s.length() <= max) ? s : s.substring(0, max) + "...(truncated)";
    }



    // contentTypeId 상수
    private static final String TYPE_ATTRACTION = "12"; // 관광지
    private static final String TYPE_STAY       = "32"; // 숙박
    private static final String TYPE_RESTAURANT = "39"; // 음식점(맛집)

    // serviceKey 인코딩 처리(기존 그대로)
    private String normalizeServiceKey(String key) {
        if (key == null) return "";
        if (key.contains("%")) return key;
        return URLEncoder.encode(key, StandardCharsets.UTF_8);
    }




    // =========================================================
    // [NEARBY 기능] locationBasedList2 호출 래퍼
    // - centerLat/centerLng/radius 기준으로 주변 목록을 가져옴
    // =========================================================
    private List<TourApiDto.Item> locationBasedList(
                                                     double centerLat, double centerLng, int radiusMeters,
                                                     String contentTypeIdOrNull, int numOfRows
    ) {
        String encodedKey = normalizeServiceKey(serviceKey);

        var builder = UriComponentsBuilder
                .fromHttpUrl(LOCATION_URL)
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "MyTripApp")
                .queryParam("_type", "json")
                .queryParam("mapY", centerLat)   // 위도
                .queryParam("mapX", centerLng)   // 경도
                .queryParam("radius", radiusMeters)
                .queryParam("arrange", "E")      // 거리순(환경에 따라 A/E 다를 수 있음)
                .queryParam("numOfRows", String.valueOf(numOfRows))
                .queryParam("pageNo", "1");

        if (contentTypeIdOrNull != null && !contentTypeIdOrNull.isBlank()) {
            builder.queryParam("contentTypeId", contentTypeIdOrNull);
        }

        URI uri = builder.build(true).toUri();
        //System.out.println("locationBasedList2 URL: " + uri);
        log.debug("locationBasedList2 URL: {}", uri); // [수정]

        try {
            TourApiDto response = restTemplate.getForObject(uri, TourApiDto.class);

            if (response != null &&
                    response.getResponse() != null &&
                    response.getResponse().getBody() != null &&
                    response.getResponse().getBody().getItems() != null &&
                    response.getResponse().getBody().getItems().getItem() != null) {
                return response.getResponse().getBody().getItems().getItem();
            }
            return Collections.emptyList();

        } catch (HttpStatusCodeException e) {
            //System.err.println("locationBasedList2 HTTP Error: " + e.getStatusCode());
            //System.err.println("locationBasedList2 Body: " + e.getResponseBodyAsString());
            // [수정] System.err -> log (status는 warn, body는 debug)
            log.warn("locationBasedList2 HTTP Error: status={}", e.getStatusCode()); // [수정]
            log.debug("locationBasedList2 Body: {}", shorten(e.getResponseBodyAsString())); // [수정]
            return Collections.emptyList();
        } catch (Exception e) {
            //e.printStackTrace();
            log.error("locationBasedList2 failed", e); // [수정]
            return Collections.emptyList();
        }
    }


    // =========================================================
    // [NEARBY 기능] /api/places/nearby 의 실제 구현
    // - categories 목록을 돌면서 locationBasedList2를 호출하고
    // - PlaceSummaryDto(title/lat/lng/category/contentId)로 정리
    // =========================================================

    public List<PlaceSummaryDto> getNearbySummaries(
                                                     double centerLat, double centerLng, int radiusMeters,
                                                     List<TourCategory> categories
    ) {
        if (categories == null || categories.isEmpty()) {
            categories = List.of(TourCategory.ATTRACTION);
        }

        List<PlaceSummaryDto> out = new java.util.ArrayList<>();

        for (TourCategory c : categories) {
            List<TourApiDto.Item> items = locationBasedList(centerLat, centerLng, radiusMeters, c.contentTypeId(),  NEARBY_ROWS_PER_CATEGORY);

            for (TourApiDto.Item it : items) {
                if (it.getTitle() == null || it.getTitle().isBlank()) continue;
                if (it.getMapy() == null || it.getMapx() == null) continue;
                if (it.getContentid() == null || it.getContentid().isBlank()) continue; // contentId 없는 건 스킵

                out.add(new PlaceSummaryDto(
                        it.getTitle(),
                        it.getMapy(),
                        it.getMapx(),
                        c.key(),
                        it.getContentid()
                ));
            }
        }

        // 중복 제거: contentId 기준
        return out.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                PlaceSummaryDto::getContentId,
                                x -> x,
                                (a, b) -> a
                        ),
                        m -> new java.util.ArrayList<>(m.values())
                ));
    }




    // =========================================================
    // [SEARCH 기능] detailCommon2 호출(overview를 얻기 위해 사용)
    // =========================================================
    private TourDetailCommonDto.Item detailCommon(String contentId) {
        String encodedKey = normalizeServiceKey(serviceKey);

        URI uri = UriComponentsBuilder
                .fromHttpUrl(DETAIL_COMMON_URL)
                .queryParam("serviceKey", encodedKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "MyTripApp")
                .queryParam("_type", "json")
                .queryParam("contentId", contentId)
                .build(true)
                .toUri();

        //System.out.println("detailCommon2 URL: " + uri);
        // [수정] println -> log.debug
        log.debug("detailCommon2 URL: {}", uri); // [수정]

        try {
            TourDetailCommonDto res = restTemplate.getForObject(uri, TourDetailCommonDto.class);
            if (res != null &&
                    res.getResponse() != null &&
                    res.getResponse().getBody() != null &&
                    res.getResponse().getBody().getItems() != null &&
                    res.getResponse().getBody().getItems().getItem() != null &&
                    !res.getResponse().getBody().getItems().getItem().isEmpty()) {
                return res.getResponse().getBody().getItems().getItem().get(0);
            }
            return null;
        } catch (HttpStatusCodeException e) { // 원인 바로 보이게 로그
            //System.err.println("detailCommon2 HTTP Error: " + e.getStatusCode());
            //System.err.println("detailCommon2 Body: " + e.getResponseBodyAsString());
            log.warn("detailCommon2 HTTP Error: status={}", e.getStatusCode()); // [수정]
            log.debug("detailCommon2 Body: {}", shorten(e.getResponseBodyAsString())); // [수정]
            return null;
        }
        catch (Exception e) {
            //e.printStackTrace();
            log.error("detailCommon2 failed", e); // [수정]
            return null;
        }
    }



    // =========================================================
    // [SEARCH 기능] 반경 기반 키워드 검색(서버 필터)
    // - TourAPI searchKeyword2는 반경 파라미터가 없어 전국검색 위험
    // - 그래서 locationBasedList2로 후보를 받고(반경 제한)
    // - 서버에서 keyword(title/addr1)로 필터링
    // =========================================================
    public List<TourApiDto.Item> searchByKeywordWithinRadius(
            double centerLat, double centerLng, int radiusMeters,
            String contentTypeId,
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) return List.of();

        //  반경 내 후보를 넉넉히 가져온 뒤 필터링 (너무 크게 잡으면 비용↑)
        // 필요하면 100~200 조절
        List<TourApiDto.Item> candidates = locationBasedList(centerLat, centerLng, radiusMeters, contentTypeId,  SEARCH_CANDIDATE_ROWS);

        if (candidates == null || candidates.isEmpty()) return List.of();

        final String kw = keyword.trim().toLowerCase();

        return candidates.stream()
                .filter(it -> it.getTitle() != null && !it.getTitle().isBlank())
                .filter(it -> {
                    String t = it.getTitle().toLowerCase();
                    String a = (it.getAddr1() == null) ? "" : it.getAddr1().toLowerCase();
                    return t.contains(kw) || a.contains(kw);
                })
                .sorted((a, b) -> {
                    double da = (a.getDist() == null) ? Double.MAX_VALUE : a.getDist();
                    double db = (b.getDist() == null) ? Double.MAX_VALUE : b.getDist();
                    return Double.compare(da, db);
                })
                .limit(30)
                .collect(Collectors.toList());
    }


    // =========================================================
    // [SEARCH 기능] overview(소개글) 캐시
    // - search 결과에 overview를 붙이려면 contentId별 detailCommon2 호출이 필요
    // - 반복 호출 비용을 줄이기 위해 캐시 적용
    //밑으로 쫙 search기능임!!!!
    // =========================================================

    // overview 캐시(동일 contentId 반복 조회 비용 절감)
    private final ConcurrentHashMap<String, OverviewCacheEntry> overviewCache = new ConcurrentHashMap<>();

    @Value("${tour.api.overview.cache-ttl-ms:86400000}") // 기본 24시간
    private long overviewCacheTtlMs;

    //  캐시 엔트리
    private static class OverviewCacheEntry {
        final String overview;
        final long expiresAt;
        OverviewCacheEntry(String overview, long expiresAt) {
            this.overview = overview;
            this.expiresAt = expiresAt;
        }
    }

    private String getOverviewCached(String contentId) {
        if (contentId == null || contentId.isBlank()) return null;

        long now = System.currentTimeMillis();
        OverviewCacheEntry cached = overviewCache.get(contentId);
        if (cached != null && cached.expiresAt > now) return cached.overview;

        TourDetailCommonDto.Item d = detailCommon(contentId);
        String overview = (d == null) ? null : d.getOverview();

        overviewCache.put(contentId, new OverviewCacheEntry(overview, now + overviewCacheTtlMs));
        return overview;
    }

    //  반경 기반 검색 + overview까지 포함해서 반환
    public List<PlaceSearchResultDto> searchByKeywordWithinRadiusWithOverview(
            double centerLat, double centerLng, int radiusMeters,
            String contentTypeId,
            String keyword
    ) {
        // 기존 필터링/정렬/limit(30) 로직 재사용
        List<TourApiDto.Item> base = searchByKeywordWithinRadius(
                centerLat, centerLng, radiusMeters, contentTypeId, keyword
        );

        if (base == null || base.isEmpty()) return List.of();

        // 각 item별 overview를 detailCommon2로 보강 (캐시 적용)
        return base.stream().map(it -> {
            PlaceSearchResultDto dto = new PlaceSearchResultDto();
            dto.setTitle(it.getTitle());
            dto.setAddr1(it.getAddr1());
            dto.setMapx(it.getMapx());
            dto.setMapy(it.getMapy());
            dto.setFirstimage(it.getFirstimage());
            dto.setContentid(it.getContentid());
            dto.setContenttypeid(it.getContenttypeid());
            dto.setDist(it.getDist());

            dto.setOverview(getOverviewCached(it.getContentid()));
            return dto;
        }).toList();
    }




}







