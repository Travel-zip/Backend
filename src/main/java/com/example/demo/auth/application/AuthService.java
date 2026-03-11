package com.example.demo.auth.application;

import com.example.demo.auth.api.ApiException;
import com.example.demo.auth.domain.UserEntity;
import com.example.demo.auth.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

// 회원가입/로그인/이메일인증 로직
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final EmailVerificationStore verificationStore;
    private final EmailSender emailSender;
    private final PasswordHasher passwordHasher;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.email.verify.code-ttl-ms:300000}")
    private long codeTtlMs;

    @Value("${app.email.verify.ok-ttl-ms:1800000}")
    private long okTtlMs;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public void sendEmailCode(String email) {
        email = normEmail(email);

        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "이메일 형식이 올바르지 않습니다.");
        }

        // 이미 가입된 이메일이면 회원가입 인증 발송을 막고 싶으면 409 (정책)
        if (userRepo.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
        }

        String code = random6digits();
        verificationStore.saveCode(email, code, codeTtlMs);

        try {
            emailSender.sendVerificationCode(email, code);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "이메일 발송에 실패했습니다.");
        }
    }

    public void verifyEmailCode(String email, String code) {
        email = normEmail(email);
        code = (code == null) ? "" : code.trim();

        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "이메일 형식이 올바르지 않습니다.");
        }
        if (code.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CODE_FORMAT", "인증코드를 입력해주세요.");
        }

        var r = verificationStore.verifyCode(email, code);
        if (r == EmailVerificationStore.VerifyResult.EXPIRED_OR_NOT_FOUND) {
            // 410 쓰면 프론트 UX가 좋지만, 요구한 범위(400/401/403/409)로 맞추려면 401/400으로 내려도 됨
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CODE_EXPIRED", "인증코드가 만료되었거나 없습니다.");
        }
        if (r == EmailVerificationStore.VerifyResult.MISMATCH) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "CODE_MISMATCH", "인증코드가 올바르지 않습니다.");
        }

        verificationStore.markVerified(email, okTtlMs);
    }

    public UserEntity signup(String loginId, String rawPassword, String email) {
        loginId = (loginId == null) ? "" : loginId.trim();
        rawPassword = (rawPassword == null) ? "" : rawPassword;
        email = normEmail(email);

        if (loginId.isBlank() || rawPassword.isBlank() || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "필수 입력값이 누락되었습니다.");
        }
        if (loginId.length() < 4 || loginId.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "아이디는 4~64자여야 합니다.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_EMAIL", "이메일 형식이 올바르지 않습니다.");
        }

        //  이메일 인증 확인
        if (!verificationStore.isVerified(email)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "이메일 인증이 필요합니다.");
        }

        // 중복 체크
        if (userRepo.existsByLoginId(loginId)) {
            throw new ApiException(HttpStatus.CONFLICT, "LOGIN_ID_ALREADY_EXISTS", "이미 사용 중인 아이디입니다.");
        }
        if (userRepo.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다.");
        }

        String hash = passwordHasher.hash(rawPassword);

        UserEntity u = new UserEntity();
        u.setLoginId(loginId);
        u.setPasswordHash(hash);
        u.setEmail(email);
        u.setEmailVerified(true);

        UserEntity saved = userRepo.save(u);

        //  회원가입 성공하면 인증 완료 표시 제거(재사용 방지)
        verificationStore.clearVerified(email);

        return saved;
    }

    public String loginAndIssueAccessToken(String loginId, String rawPassword) {
        loginId = (loginId == null) ? "" : loginId.trim();
        rawPassword = (rawPassword == null) ? "" : rawPassword;

        if (loginId.isBlank() || rawPassword.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "아이디/비밀번호를 입력해주세요.");
        }

        var u = userRepo.findByLoginId(loginId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!u.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "이메일 인증이 필요합니다.");
        }

        if (!passwordHasher.matches(rawPassword, u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return jwtTokenProvider.createAccessToken(u.getId(), u.getLoginId());
    }

    private String random6digits() {
        int n = (int)(Math.random() * 900000) + 100000;
        return String.valueOf(n);
    }

    private String normEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
