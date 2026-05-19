package com.example.demo.map.application;

import com.example.demo.chat.application.RoomService;
import com.example.demo.map.api.dto.PlaceAddListRequestDto;
import com.example.demo.map.api.dto.PlaceAddRequestDto;
import com.example.demo.map.api.dto.PlaceSearchResultDto;
import com.example.demo.map.api.dto.PlaceSummaryDto;
import com.example.demo.map.domain.TourPlaceEntity;
import com.example.demo.map.domain.TourPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourPlaceService {

    private final RoomService roomService;
    private final TourPlaceRepository tourRepo;

    public void saveSearchResults(String roomId, List<PlaceSearchResultDto> results) {
        if (results == null || results.isEmpty()) return;

        var room = roomService.getOrCreate(roomId);
        roomService.touch(roomId);

        for (var dto : results) {
            if (dto.getTitle() == null || dto.getTitle().isBlank()) continue;
            if (dto.getMapy() == null || dto.getMapx() == null) continue;

            Double lat = dto.getMapy(); // 위도
            Double lng = dto.getMapx(); // 경도


            if (tourRepo.existsByRoom_RoomIdAndTitleAndLatAndLng(roomId, dto.getTitle(), lat, lng)) continue;

            TourPlaceEntity e = new TourPlaceEntity();
            e.setRoom(room);
            e.setTitle(dto.getTitle());
            e.setLat(lat);
            e.setLng(lng);
            e.setImageUrl(dto.getFirstimage()); // [사진추가] Search 결과 이미지


            tourRepo.save(e);
        }
    }


    public void saveNearbyResults(String roomId, List<PlaceSummaryDto> results) {
        if (results == null || results.isEmpty()) return;

        var room = roomService.getOrCreate(roomId);
        roomService.touch(roomId);

        for (var dto : results) {
            if (dto.getTitle() == null || dto.getTitle().isBlank()) continue;
            if (dto.getLat() == null || dto.getLng() == null) continue;

            if (tourRepo.existsByRoom_RoomIdAndTitleAndLatAndLng(roomId, dto.getTitle(), dto.getLat(), dto.getLng())) continue;

            TourPlaceEntity e = new TourPlaceEntity();
            e.setRoom(room);
            e.setTitle(dto.getTitle());
            e.setLat(dto.getLat());
            e.setLng(dto.getLng());
            e.setImageUrl(dto.getImageUrl()); // [사진추가] Nearby 결과 이미지


            tourRepo.save(e);
        }
    }


    public String buildCandidateList(String roomId, int limit) {
        List<TourPlaceEntity> list = tourRepo.findByRoom_RoomIdOrderByCreatedAtDesc(roomId);
        return list.stream()
                .limit(limit)
                .map(p -> "- " + p.getTitle() + " (lat=" + p.getLat() + ", lng=" + p.getLng()
                        +", image=" + (p.getImageUrl() != null ? p.getImageUrl() : "")+ ")")  // [사진추가]
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }

    // =========================================================
    // [추가] 프론트엔드에서 수동으로 전달한 장소를 DB에 저장
    // =========================================================
    public void addManualPlace(PlaceAddRequestDto dto) {
        if (dto == null) return;

        String roomId = (dto.getRoomId() == null || dto.getRoomId().isBlank()) ? "default" : dto.getRoomId();

        // 1. 방 정보 가져오기 (없으면 생성) 및 터치(업데이트 타임 갱신)
        var room = roomService.getOrCreate(roomId);
        roomService.touch(roomId);

        // 2. 필수 값 검증
        if (dto.getTitle() == null || dto.getTitle().isBlank()) return;
        if (dto.getLat() == null || dto.getLng() == null) return;

        // 3. 중복 방지 (동일한 방에 같은 이름/위치면 저장 안 함)
        if (tourRepo.existsByRoom_RoomIdAndTitleAndLatAndLng(roomId, dto.getTitle(), dto.getLat(), dto.getLng())) {
            return;
        }

        // 4. 엔티티 생성 및 저장
        TourPlaceEntity e = new TourPlaceEntity();
        e.setRoom(room);
        e.setTitle(dto.getTitle());
        e.setLat(dto.getLat());
        e.setLng(dto.getLng());
        // [수정] 프론트에서 imageUrl을 보내지 않으므로 세팅 생략 (DB에는 null로 저장됨)

        tourRepo.save(e);
    }

    // =========================================================
    // [추가] 프론트엔드에서 여러 장소를 한 번에 배열로 보내면 일괄 저장
    // =========================================================
    public void addManualPlaces(PlaceAddListRequestDto dto) {
        if (dto == null || dto.getPlaces() == null || dto.getPlaces().isEmpty()) return;

        String roomId = (dto.getRoomId() == null || dto.getRoomId().isBlank()) ? "default" : dto.getRoomId();

        // 1. 방 정보 가져오기 (없으면 생성) 및 터치(업데이트 타임 갱신)
        var room = roomService.getOrCreate(roomId);
        roomService.touch(roomId);

        // 저장할 엔티티들을 모아둘 리스트
        List<TourPlaceEntity> entitiesToSave = new ArrayList<>();

        for (PlaceAddListRequestDto.PlaceItem item : dto.getPlaces()) {
            // 2. 필수 값 검증
            if (item.getTitle() == null || item.getTitle().isBlank()) continue;
            if (item.getLat() == null || item.getLng() == null) continue;

            // 3. 중복 방지 (동일한 방에 같은 이름/위치면 스킵)
            if (tourRepo.existsByRoom_RoomIdAndTitleAndLatAndLng(roomId, item.getTitle(), item.getLat(), item.getLng())) {
                continue;
            }

            // 4. 엔티티 생성 후 리스트에 추가
            TourPlaceEntity e = new TourPlaceEntity();
            e.setRoom(room);
            e.setTitle(item.getTitle());
            e.setLat(item.getLat());
            e.setLng(item.getLng());

            entitiesToSave.add(e);
        }

        // 5. 유효한 장소들만 saveAll을 통해 DB에 한 번에 저장 (성능 최적화)
        if (!entitiesToSave.isEmpty()) {
            tourRepo.saveAll(entitiesToSave);
        }
    }
}
