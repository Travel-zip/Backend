package com.example.demo.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

//  SMTP로 실제 이메일 발송
@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username}}") // app.mail.from 없으면 spring.mail.username 사용
    private String from;

    @Override
    public void sendVerificationCode(String email, String code) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, "UTF-8");

            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("[TravelZip] 이메일 인증 코드");
            helper.setText(
                    "안녕하세요!\n\n" +
                            "아래 인증 코드를 입력해주세요.\n\n" +
                            "인증 코드: " + code + "\n\n" +
                            "감사합니다.",
                    false
            );

            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("메일 발송 실패", e);
        }
    }
}
