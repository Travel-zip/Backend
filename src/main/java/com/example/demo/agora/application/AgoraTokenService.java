package com.example.demo.agora.application;

// 1. 스프링의 @Value를 사용하도록 수정
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// 2. 아고라 관련 클래스 Import 추가
import io.agora.media.RtcTokenBuilder2;
import io.agora.media.RtcTokenBuilder2.Role;

@Service
public class AgoraTokenService {

    // 3. application.properties에 적은 키값(app.agora.~)과 정확히 일치시킴
    @Value("${app.agora.app-id}")
    private String appId;

    @Value("${app.agora.app-certificate}")
    private String appCertificate;

    // 토큰 만료 시간 설정 (예: 24시간 = 86400초)
    private static final int EXPIRATION_TIME_IN_SECONDS = 3600;

    public String generateToken(String roomId, String userId) {
        RtcTokenBuilder2 tokenBuilder = new RtcTokenBuilder2();

        // 현재 시간 기준으로 언제 토큰이 만료될지 계산
        int timestamp = (int) (System.currentTimeMillis() / 1000 + EXPIRATION_TIME_IN_SECONDS);

        // 프론트엔드에서 유저 ID를 String으로 보내므로 buildTokenWithUserAccount 메서드 사용
        String result = tokenBuilder.buildTokenWithUserAccount(
                appId,
                appCertificate,
                roomId,        // 채널명 (프론트의 roomId)
                userId,        // 유저 계정명 (프론트의 myLoginId)
                Role.ROLE_PUBLISHER, // 말하기/듣기 모두 가능한 권한
                timestamp,     // 토큰 자체의 만료 시간
                timestamp      // 권한(Publisher)의 만료 시간
        );

        return result;
    }
}