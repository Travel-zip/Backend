package com.example.demo.auth.application;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Redis 없이도 돌아가는 InMemory 인증 저장소(TTL 지원)
@Component
public class InMemoryEmailVerificationStore implements EmailVerificationStore {

    private static class Entry {
        final String value;
        final long expiresAt;
        Entry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, Entry> codeMap = new ConcurrentHashMap<>();
    private final Map<String, Entry> verifiedMap = new ConcurrentHashMap<>();

    @Override
    public void saveCode(String email, String code, long ttlMs) {
        long exp = System.currentTimeMillis() + ttlMs;
        codeMap.put(norm(email), new Entry(code, exp));
    }

    @Override
    public VerifyResult verifyCode(String email, String code) {
        String key = norm(email);
        Entry e = codeMap.get(key);
        if (e == null) return VerifyResult.EXPIRED_OR_NOT_FOUND;
        if (e.expiresAt < System.currentTimeMillis()) {
            codeMap.remove(key);
            return VerifyResult.EXPIRED_OR_NOT_FOUND;
        }
        if (e.value == null || !e.value.equals(code)) {
            return VerifyResult.MISMATCH;
        }
        // 성공하면 코드 제거(재사용 방지)
        codeMap.remove(key);
        return VerifyResult.OK;
    }

    @Override
    public void markVerified(String email, long ttlMs) {
        long exp = System.currentTimeMillis() + ttlMs;
        verifiedMap.put(norm(email), new Entry("true", exp));
    }

    @Override
    public boolean isVerified(String email) {
        String key = norm(email);
        Entry e = verifiedMap.get(key);
        if (e == null) return false;
        if (e.expiresAt < System.currentTimeMillis()) {
            verifiedMap.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public void clearVerified(String email) {
        verifiedMap.remove(norm(email));
    }

    private String norm(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
