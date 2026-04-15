package com.example.demo.agora.api;

import com.example.demo.agora.application.AgoraTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AgoraController {

    private final AgoraTokenService agoraTokenService;

    @GetMapping("/api/agora/token")
    public ResponseEntity<Map<String, String>> getAgoraToken(
            @RequestParam("roomId") String roomId,
            @RequestParam("userId") String userId) {

        // 1. 서비스 로직을 통해 토큰 생성
        String token = agoraTokenService.generateToken(roomId, userId);

        // 2. 프론트엔드가 기대하는 JSON 형태로 매핑 { "token": "동적토큰값" }
        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }
}
