package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LegacyTokenUtil {
    public static final int TOKEN_TTL_SECONDS = 3600;
    public static final String ISSUER = "workshop-auth-backend";
    public static final String AUDIENCE = "workshop-requirements-api";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";
    private static final ObjectMapper JSON = new ObjectMapper();

    private LegacyTokenUtil() {}

    public static void validateSecret(String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("AUTH_TOKEN_SECRET must contain at least 32 bytes");
        }
    }

    public static String create(String username, String role, String client, String secret) {
        validateSecret(secret);
        long issuedAt = Instant.now().getEpochSecond();

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", ISSUER);
        claims.put("aud", AUDIENCE);
        claims.put("sub", username);
        claims.put("role", role);
        claims.put("client", client);
        claims.put("iat", issuedAt);
        claims.put("exp", issuedAt + TOKEN_TTL_SECONDS);

        String signingInput = encodeJson(header) + "." + encodeJson(claims);
        return signingInput + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sign(signingInput, secret));
    }

    private static String encodeJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(JSON.writeValueAsBytes(value));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to serialize token", error);
        }
    }

    private static byte[] sign(String signingInput, String secret) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SIGNATURE_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("Unable to sign token", error);
        }
    }
}
