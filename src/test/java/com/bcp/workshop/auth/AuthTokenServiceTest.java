package com.bcp.workshop.auth;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTokenServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final String TEST_SECRET = "test-only-workshop-auth-key-32-bytes";

    @Test
    void rejectsMissingSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new AuthTokenService(null, FIXED_CLOCK));
    }

    @Test
    void rejectsSigningSecretShorterThan256Bits() {
        assertThrows(IllegalStateException.class, () -> new AuthTokenService("too-short", FIXED_CLOCK));
    }

    @Test
    void acceptsExplicitStrongSigningSecret() {
        assertDoesNotThrow(() -> new AuthTokenService(TEST_SECRET, FIXED_CLOCK));
    }

    @Test
    void issuesSignedTokenWithRequiredClaimsAndOneHourLifetime() {
        AuthTokenService service = new AuthTokenService(TEST_SECRET, FIXED_CLOCK);
        String token = service.issue("student", "angular-workshop");

        DecodedJWT verified = service.verify(token);
        assertEquals("HS256", verified.getAlgorithm(), "token uses HS256");
        assertEquals("student", verified.getSubject(), "subject identifies the user");
        assertEquals("angular-workshop", verified.getClaim("client").asString(), "client claim preserves input");
        assertEquals(Date.from(FIXED_CLOCK.instant()), verified.getIssuedAt(), "issued-at matches the issuance instant");
        assertEquals(Date.from(FIXED_CLOCK.instant().plusSeconds(AuthTokenService.TOKEN_LIFETIME_SECONDS)),
                verified.getExpiresAt(), "expiration is one hour after issuance");

        String payload = new String(Base64.getUrlDecoder().decode(verified.getPayload()), StandardCharsets.UTF_8);
        assertFalse(payload.contains(TEST_SECRET), "token payload excludes signing secret");
    }

    @Test
    void rejectsTokenWhenSignedContentsAreChanged() {
        AuthTokenService service = new AuthTokenService(TEST_SECRET, FIXED_CLOCK);
        String token = service.issue("student", "angular-workshop");
        String[] segments = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(segments[1]), StandardCharsets.UTF_8);
        String changedPayload = payload.replace("angular-workshop", "changed-client");
        assertFalse(payload.equals(changedPayload), "test mutation changes a signed claim");
        String changedSegment = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(changedPayload.getBytes(StandardCharsets.UTF_8));
        String alteredToken = segments[0] + "." + changedSegment + "." + segments[2];

        assertThrows(JWTVerificationException.class, () -> service.verify(alteredToken),
                "altered signed contents are rejected");
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        AuthTokenService issuer = new AuthTokenService(TEST_SECRET, FIXED_CLOCK);
        AuthTokenService verifier = new AuthTokenService("another-test-only-key-32-bytes-long", FIXED_CLOCK);
        String token = issuer.issue("student", "angular-workshop");

        assertThrows(JWTVerificationException.class, () -> verifier.verify(token),
                "invalid signature is rejected");
    }

    @Test
    void acceptsTokenImmediatelyBeforeExpiration() {
        Instant expiresAt = FIXED_CLOCK.instant().plusSeconds(AuthTokenService.TOKEN_LIFETIME_SECONDS);
        String token = new AuthTokenService(TEST_SECRET, FIXED_CLOCK).issue("student", "angular-workshop");
        AuthTokenService verifier = new AuthTokenService(
                TEST_SECRET,
                Clock.fixed(expiresAt.minusSeconds(1), ZoneOffset.UTC)
        );

        assertDoesNotThrow(() -> verifier.verify(token));
    }

    @Test
    void rejectsTokenAtAndAfterExpiration() {
        Instant expiresAt = FIXED_CLOCK.instant().plusSeconds(AuthTokenService.TOKEN_LIFETIME_SECONDS);
        String token = new AuthTokenService(TEST_SECRET, FIXED_CLOCK).issue("student", "angular-workshop");
        AuthTokenService atExpiration = new AuthTokenService(TEST_SECRET, Clock.fixed(expiresAt, ZoneOffset.UTC));
        AuthTokenService afterExpiration = new AuthTokenService(TEST_SECRET, Clock.fixed(expiresAt.plusSeconds(1), ZoneOffset.UTC));

        assertThrows(TokenExpiredException.class, () -> atExpiration.verify(token), "exp is exclusive");
        assertThrows(TokenExpiredException.class, () -> afterExpiration.verify(token), "expired token is rejected");
    }
}