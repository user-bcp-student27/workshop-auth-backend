package com.bcp.workshop.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@ApplicationScoped
@Startup
public class AuthTokenService {
    static final int MINIMUM_SIGNING_KEY_BYTES = 32;
    static final long TOKEN_LIFETIME_SECONDS = 3600;

    private final Algorithm signingAlgorithm;
    private final Clock clock;

    @Inject
    public AuthTokenService(@ConfigProperty(name = "auth.token.secret") String signingSecret) {
        this(signingSecret, Clock.systemUTC());
    }

    AuthTokenService(String signingSecret, Clock clock) {
        if (signingSecret == null || signingSecret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SIGNING_KEY_BYTES) {
            throw new IllegalStateException("AUTH_TOKEN_SECRET must contain at least 32 bytes");
        }
        this.signingAlgorithm = Algorithm.HMAC256(signingSecret);
        this.clock = clock;
    }

    public String issue(String subject, String client) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(TOKEN_LIFETIME_SECONDS);
        return JWT.create()
                .withSubject(subject)
                .withClaim("client", client)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(signingAlgorithm);
    }

    public DecodedJWT verify(String token) throws JWTVerificationException {
        JWTVerifier.BaseVerification verification = (JWTVerifier.BaseVerification) JWT.require(signingAlgorithm);
        return verification.build(clock).verify(token);
    }
}