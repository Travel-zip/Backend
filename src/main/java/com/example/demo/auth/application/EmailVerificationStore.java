package com.example.demo.auth.application;

// 인증코드/인증완료 상태 저장소(현재는 InMemory)
public interface EmailVerificationStore {

    void saveCode(String email, String code, long ttlMs);

    VerifyResult verifyCode(String email, String code);

    void markVerified(String email, long ttlMs);

    boolean isVerified(String email);

    void clearVerified(String email);

    enum VerifyResult {
        OK,
        MISMATCH,
        EXPIRED_OR_NOT_FOUND
    }
}
