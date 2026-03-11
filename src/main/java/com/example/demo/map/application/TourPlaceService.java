package com.example.demo.map.application;

import com.example.demo.chat.application.RoomService;
import com.example.demo.map.api.dto.PlaceSearchResultDto;
import com.example.demo.map.api.dto.PlaceSummaryDto;
import com.example.demo.map.domain.TourPlaceEntity;
import com.example.demo.map.domain.TourPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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


            tourRepo.save(e);
        }
    }


    public String buildCandidateList(String roomId, int limit) {
        List<TourPlaceEntity> list = tourRepo.findByRoom_RoomIdOrderByCreatedAtDesc(roomId);
        return list.stream()
                .limit(limit)
                .map(p -> "- " + p.getTitle() + " (lat=" + p.getLat() + ", lng=" + p.getLng() + ")")
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }
}
