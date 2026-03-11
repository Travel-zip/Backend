package com.example.demo.auth.api;

import com.example.demo.auth.api.dto.AuthDtos;
import com.example.demo.auth.application.AuthService;
import com.example.demo.auth.domain.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.example.demo.auth.api.dto.AuthDtos.*;

// 인증 API (이메일 인증 + 회원가입 + 로그인)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // 1) 이메일 인증코드 발송
    @PostMapping("/email/send-code")
    public SimpleOkResponse sendCode(@RequestBody EmailSendCodeRequest req) {
        authService.sendEmailCode(req.getEmail());
        return new SimpleOkResponse(true);
    }

    // 2) 이메일 인증코드 확인
    @PostMapping("/email/verify-code")
    public SimpleOkResponse verifyCode(@RequestBody EmailVerifyCodeRequest req) {
        authService.verifyEmailCode(req.getEmail(), req.getCode());
        return new SimpleOkResponse(true);
    }

    //  3) 회원가입
    @PostMapping("/signup")
    public SignupResponse signup(@RequestBody SignupRequest req) {
        UserEntity u = authService.signup(req.getLoginId(), req.getPassword(), req.getEmail());
        return new SignupResponse(u.getId(), u.getLoginId(), u.getEmail());
    }

    //  4) 로그인 (AccessToken만)
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        String access = authService.loginAndIssueAccessToken(req.getLoginId(), req.getPassword());

        // user 정보도 내려주면 프론트가 편함
        // (여기서는 loginId/email을 다시 조회하고 싶으면 서비스에서 UserEntity도 같이 반환하도록 바꿔도 됨)
        // MVP라 단순히 loginId만 응답에 포함
        return new LoginResponse(access, new UserSummary(null, req.getLoginId(), null));
    }
}
