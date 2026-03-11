package com.example.demo.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//  Auth 관련 DTO 모음 (파일 분리하기 귀찮으면 이렇게 한 파일에 모아도 됨)
public class AuthDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailSendCodeRequest {
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmailVerifyCodeRequest {
        private String email;
        private String code;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {
        private String loginId;
        private String password;
        private String email;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        private String loginId;
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class SimpleOkResponse {
        private boolean ok;
    }

    @Data
    @AllArgsConstructor
    public static class SignupResponse {
        private Long userId;
        private String loginId;
        private String email;
    }

    @Data
    @AllArgsConstructor
    public static class UserSummary {
        private Long userId;
        private String loginId;
        private String email;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResponse {
        private String accessToken;
        private UserSummary user;
    }
}
