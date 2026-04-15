package com.example.demo.schedule.api;

import com.example.demo.auth.api.ApiException;
import com.example.demo.auth.application.AuthRequestAttr;
import com.example.demo.schedule.application.TravelPlanService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/travel-plan")
public class TravelPlanApiController {

    private final TravelPlanService travelPlanService;

    // GET /api/rooms/{roomId}/travel-plan/latest
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestPlan(
            @PathVariable String roomId,
            HttpServletRequest request
    ) {
        // 1. 로그인 여부 확인
        Long userId = (Long) request.getAttribute(AuthRequestAttr.USER_ID);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        // 2. 가장 최근 계획 조회
        String planJson = travelPlanService.getLatestPlanJson(roomId);

        // 3. 만들어진 계획이 없다면 204 (No Content) 반환
        if (planJson == null) {
            return ResponseEntity.noContent().build();
        }

        // 4. 계획이 있다면, 이미 JSON 형태의 문자열이므로 그대로 바디에 담아 반환
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(planJson);
    }
}
