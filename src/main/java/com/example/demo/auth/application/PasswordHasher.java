package com.example.demo.auth.application;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

// Spring Security 의존 없이 PBKDF2 해싱 사용
@Component
public class PasswordHasher {

    private static final String ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000; // 학부 프로젝트에 충분
    private static final int SALT_LEN = 16;
    private static final int KEY_LEN = 256;

    private final SecureRandom random = new SecureRandom();

    public String hash(String rawPassword) {
        if (rawPassword == null) rawPassword = "";
        byte[] salt = new byte[SALT_LEN];
        random.nextBytes(salt);

        byte[] derived = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LEN);

        // 저장 포맷: pbkdf2$iterations$saltBase64$hashBase64
        return "pbkdf2$" + ITERATIONS + "$" +
                Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$" +
                Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
    }

    public boolean matches(String rawPassword, String stored) {
        if (stored == null || !stored.startsWith("pbkdf2$")) return false;
        try {
            String[] parts = stored.split("\\$");
            int it = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] hash = Base64.getUrlDecoder().decode(parts[3]);

            byte[] derived = pbkdf2((rawPassword == null ? "" : rawPassword).toCharArray(), salt, it, hash.length * 8);

            // 상수시간 비교
            if (derived.length != hash.length) return false;
            int diff = 0;
            for (int i = 0; i < derived.length; i++) diff |= (derived[i] ^ hash[i]);
            return diff == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLenBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLenBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGO);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
}
