package com.example.demo.schedule.application;

import com.example.demo.chat.application.RoomService;
import com.example.demo.schedule.domain.TravelPlanEntity;
import com.example.demo.schedule.domain.TravelPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TravelPlanService {

    private final RoomService roomService;
    private final TravelPlanRepository planRepo;

    public void save(String roomId, String planJson) {
        var room = roomService.getOrCreate(roomId);
        roomService.touch(roomId);

        TravelPlanEntity e = new TravelPlanEntity();
        e.setRoom(room);
        e.setPlanJson(planJson);
        planRepo.save(e);
    }

    // 가장 최근 여행 계획 JSON 불러오기
    public String getLatestPlanJson(String roomId) {
        return planRepo.findTopByRoom_RoomIdOrderByCreatedAtDesc(roomId)
                .map(TravelPlanEntity::getPlanJson)
                .orElse(null); // 만들어진 계획이 없으면 null 반환
    }
}
