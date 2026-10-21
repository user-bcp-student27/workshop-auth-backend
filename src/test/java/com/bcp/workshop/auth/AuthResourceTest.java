package com.bcp.workshop.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;

@QuarkusTest
class AuthResourceTest {
    @Inject
    AuthTokenService tokenService;

    @Test
    void logsInWithWorkshopUser() {
        Response response = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login");

        assertEquals(200, response.statusCode(), "valid credentials return success");
        Map<String, Object> body = response.jsonPath().getMap("$");
        assertEquals(Set.of("token", "expiresIn", "user", "debug"), body.keySet(), "success response fields");
        assertTrue(body.get("token") instanceof String token && !token.isBlank(), "token is present");
        assertEquals(3600, ((Number) body.get("expiresIn")).intValue(), "expiresIn remains 3600 seconds");
        assertEquals(Map.of(
                "username", "student",
                "displayName", "Workshop Student",
                "role", "STUDENT"
        ), body.get("user"), "user response fields");
        assertEquals("authenticated-with-legacy-flow", body.get("debug"), "debug response value");

        DecodedJWT decodedToken = tokenService.verify((String) body.get("token"));
        assertEquals("student", decodedToken.getSubject(), "token subject identifies the authenticated user");
        assertEquals("angular-workshop", decodedToken.getClaim("client").asString(), "token includes the accepted client");
        assertTrue(decodedToken.getIssuedAt() != null, "token includes issued-at");
        assertTrue(decodedToken.getExpiresAt() != null, "token includes expiration");
        long remainingLifetime = decodedToken.getExpiresAt().toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(Math.abs(3600 - remainingLifetime) <= 1, "expiresIn agrees with exp minus now within one second");
    }

    @Test
        void returns400WhenClientIsMissingNullOrBlank() {
        assertErrorResponse(
            postLogin("{\"username\":\"student\",\"password\":\"Workshop123!\"}"),
            400,
            "Client is required"
        );
        assertErrorResponse(
            postLogin("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":null}"),
            400,
            "Client is required"
        );
        assertErrorResponse(
            postLogin("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"\"}"),
            400,
            "Client is required"
        );
        assertErrorResponse(
            postLogin("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"   \"}"),
            400,
            "Client is required"
        );
    }

    @Test
    void preservesAcceptedClientValueInTokenClaim() {
        Response response = postLogin("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\" mobile app \"}");

        assertEquals(200, response.statusCode(), "non-blank client is accepted");
        DecodedJWT decodedToken = tokenService.verify(response.jsonPath().getString("token"));
        assertEquals(" mobile app ", decodedToken.getClaim("client").asString(), "client value is not normalized");
    }

    @Test
    void authenticationLogsExcludeTokensSecretsAndPasswords() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        CapturingLogHandler handler = new CapturingLogHandler();
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);
        String issuedToken = null;
        try {
            Response success = postLogin("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"log-test-client\"}");
            assertEquals(200, success.statusCode(), "successful login remains available");
            issuedToken = success.jsonPath().getString("token");
            assertTrue(issuedToken != null && !issuedToken.isBlank(), "successful login issues a token");
            tokenService.verify(issuedToken);

            Response failure = postLogin("{\"username\":\"unknown-student\",\"password\":\"incorrect-password-marker\",\"client\":\"log-test-client\"}");
            assertEquals(401, failure.statusCode(), "invalid credentials remain unauthorized");
        } finally {
            rootLogger.removeHandler(handler);
        }

        String capturedLogs = handler.contents();
        String signingSecret = ConfigProvider.getConfig().getValue("auth.token.secret", String.class);
        assertFalse(capturedLogs.contains(issuedToken), "captured logs exclude the issued token");
        assertFalse(capturedLogs.contains(signingSecret), "captured logs exclude the signing key");
        assertFalse(capturedLogs.contains("Workshop123!"), "captured logs exclude the submitted password");
        assertFalse(capturedLogs.contains("incorrect-password-marker"), "captured logs exclude the rejected password");
    }

    @Test
    void returns400WhenRequestIsNull() {
        assertErrorResponse(postLogin("null"), 400, "Request is null");
    }

    @Test
    void returns400WhenUsernameIsMissingOrBlank() {
        assertErrorResponse(postLogin("{\"password\":\"Workshop123!\"}"), 400, "Username is required");
        assertErrorResponse(postLogin("{\"username\":\"   \",\"password\":\"Workshop123!\"}"), 400, "Username is required");
    }

    @Test
    void returns400WhenPasswordIsMissingOrBlank() {
        assertErrorResponse(postLogin("{\"username\":\"student\"}"), 400, "Password is required");
        assertErrorResponse(postLogin("{\"username\":\"student\",\"password\":\" \"}"), 400, "Password is required");
    }

    @Test
    void returns401WhenCredentialsAreInvalid() {
        assertErrorResponse(
                postLogin("{\"username\":\"unknown-student\",\"password\":\"incorrect\",\"client\":\"test-client\"}"),
                401,
                "Invalid user unknown-student or password"
        );
    }

    @Test
    void escapesDisplayNameInSuccessfulResponse() {
        AuthResource resource = new AuthResource();
        resource.database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return new UserRecord(1, username, "<Student & Coach>", "STUDENT");
            }
        };
        resource.tokenService = tokenService;
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";
        request.client = "test-client";

        jakarta.ws.rs.core.Response response = resource.login(request);
        assertEquals(200, response.getStatus(), "valid credentials return success");
        Map<?, ?> body = (Map<?, ?>) response.getEntity();
        Map<?, ?> user = (Map<?, ?>) body.get("user");
        assertEquals("&lt;Student &amp; Coach&gt;", user.get("displayName"), "display name is HTML-escaped");
    }

    @Test
    void preservesUnexpectedFailureResponseShape() {
        AuthResource resource = new AuthResource();
        resource.database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                throw new IllegalStateException("controlled failure");
            }
        };
        resource.tokenService = tokenService;
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";
        request.client = "test-client";

        jakarta.ws.rs.core.Response response = resource.login(request);
        assertEquals(500, response.getStatus(), "unexpected failures return server error");
        Map<?, ?> body = (Map<?, ?>) response.getEntity();
        assertEquals(Set.of("message", "exception"), body.keySet(), "server error response fields");
        assertEquals("controlled failure", body.get("message"), "server error message");
        assertEquals(IllegalStateException.class.getName(), body.get("exception"), "exception type");
    }

    private Response postLogin(String body) {
        return given()
                .contentType("application/json")
                .body(body)
                .when().post("/auth/login");
    }

    private void assertErrorResponse(Response response, int status, String message) {
        assertEquals(status, response.statusCode(), "documented login error status");
        assertEquals(message, response.jsonPath().getString("message"), "documented login error message");
    }

    private static final class CapturingLogHandler extends Handler {
        private final StringBuilder contents = new StringBuilder();
        private final SimpleFormatter formatter = new SimpleFormatter();

        @Override
        public synchronized void publish(LogRecord record) {
            if (isLoggable(record)) {
                contents.append(formatter.format(record));
            }
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}

        synchronized String contents() {
            return contents.toString();
        }
    }
}
