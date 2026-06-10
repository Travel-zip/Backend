package com.example.demo.ennoia;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ennoia")
public class EnnoiaProxyController {

    @Value("${ennoia.api-key}")
    private String apiKey;

    @Value("${ennoia.project}")
    private String project;

    @Value("${ennoia.user-id}")
    private String userId;

    @PostMapping("/chat")
    public ResponseEntity<String> chatProxy(@RequestBody Map<String, Object> requestBody) {
        String url = "https://api.ennoia.so/api/preset/v2/chat/completions";
        RestTemplate restTemplate = new RestTemplate();

        // 1. HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apiKey", apiKey);
        headers.set("project", project);

        // 헤더 유저 ID 세팅 (유진님 인증키 값 주입)
        headers.set("X-ENNOIA-USER-ID", userId);

        // 🚨 (주의) HTTP 헤더 인코딩 문제 방지를 위해 기존 'x-mcp-한국관광공사-authorization'은 깨끗하게 삭제했습니다!

        // 🚀 [최종 필살기] 요청 바디(JSON) 최상단에 userId 값을 직접 강제 추가
        requestBody.put("userId", userId);

        // 2. 헤더와 바디 결합
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 3. 외부 API(엔노이아)로 요청 전송
        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Proxy failed\", \"reason\": \"" + e.getMessage() + "\"}");
        }
    }
}

