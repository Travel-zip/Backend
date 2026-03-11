package com.example.demo.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

//  HS256 JWT 생성/검증 (Access Token만)
@Component
public class JwtTokenProvider {

    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-ttl-ms:1800000}")
    private long accessTtlMs;

    public String createAccessToken(Long userId, String loginId) {
        long nowSec = System.currentTimeMillis() / 1000L;
        long expSec = (System.currentTimeMillis() + accessTtlMs) / 1000L;

        try {
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String payloadJson = om.writeValueAsString(Map.of(
                    "sub", String.valueOf(userId),
                    "loginId", loginId,
                    "iat", nowSec,
                    "exp", expSec
            ));

            String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
            String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = header + "." + payload;
            String sig = hmacSha256(signingInput, secret);

            return signingInput + "." + sig;
        } catch (Exception e) {
            throw new RuntimeException("JWT create failed", e);
        }
    }

    public Map<String, Object> validateAndGetClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new RuntimeException("Invalid token");

            String signingInput = parts[0] + "." + parts[1];
            String expectedSig = hmacSha256(signingInput, secret);
            if (!constantTimeEquals(expectedSig, parts[2])) {
                throw new RuntimeException("Invalid signature");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = om.readValue(payloadBytes, Map.class);

            long nowSec = System.currentTimeMillis() / 1000L;
            Object expObj = claims.get("exp");
            long exp = expObj instanceof Number ? ((Number) expObj).longValue() : Long.parseLong(String.valueOf(expObj));
            if (exp < nowSec) throw new RuntimeException("Token expired");

            return claims;
        } catch (Exception e) {
            throw new RuntimeException("JWT invalid: " + e.getMessage(), e);
        }
    }

    private String hmacSha256(String data, String secretKey) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return base64Url(sig);
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) diff |= (a.charAt(i) ^ b.charAt(i));
        return diff == 0;
    }
}
