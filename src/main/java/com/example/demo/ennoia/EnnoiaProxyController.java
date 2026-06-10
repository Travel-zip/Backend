package com.example.demo.ennoia;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/ennoia")
@CrossOrigin(origins = "*") // 🌟 프론트엔드 CORS 에러 해결 치트키
public class EnnoiaProxyController {

    @Value("${ennoia.api-key}")
    private String apiKey;

    @Value("${ennoia.project}")
    private String project;

    @Value("${ennoia.user-id}")
    private String userId;

    @PostMapping("/chat")
    public ResponseEntity<String> chatProxy(@RequestBody Map<String, Object> requestBody) {
        // 🌟 [핵심] 프론트에서 우리 서버로 요청이 무사히 도착했는지 터미널에서 확인하기 위한 로그!
        System.out.println("🟢 [프록시 성공] 프론트엔드에서 요청이 정상적으로 들어왔습니다!");

        String url = "https://api.ennoia.so/api/preset/v2/chat/completions";
        RestTemplate restTemplate = new RestTemplate();

        // 1. HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("apiKey", apiKey);
        headers.set("project", project);
        headers.set("X-ENNOIA-USER-ID", userId);

        // 2. 바디에 userId 강제 주입
        requestBody.put("userId", userId);

        // 3. 헤더와 바디 결합
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 4. 외부 API(엔노이아)로 요청 전송
        try {
            System.out.println("🚀 엔노이아 서버로 요청 쏘는 중...");
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("✅ 엔노이아 응답 성공!");

            // 🌟 [핵심 해결책] Nginx의 chunked 에러 방지를 위해, 헤더를 다 버리고 '순수 바디 내용'만 새로운 200 OK로 다시 포장해서 리턴합니다!
            return ResponseEntity.ok().body(response.getBody());
        } catch (Exception e) {
            System.out.println("❌ 엔노이아 통신 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Proxy failed\", \"reason\": \"" + e.getMessage() + "\"}");
        }
    }
}
