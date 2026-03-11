package com.example.demo.auth.application;

// 이메일 전송 추상화(지금은 콘솔 출력). 나중에 SMTP 붙이기 쉬움.
public interface EmailSender {
    void sendVerificationCode(String email, String code);
}
