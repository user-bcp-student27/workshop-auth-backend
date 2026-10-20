package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class AuthResourceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TEST_SECRET = "test-only-secret-with-at-least-32-bytes";

    @Test
    void logsInWithWorkshopUserAndIssuesSignedTokenWithRequiredClaims() throws Exception {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("token", notNullValue())
                .body("expiresIn", is(LegacyTokenUtil.TOKEN_TTL_SECONDS))
                .extract().path("token");

        String[] segments = token.split("\\.");
        assertEquals(3, segments.length);
        JsonNode header = JSON.readTree(Base64.getUrlDecoder().decode(segments[0]));
        JsonNode claims = JSON.readTree(Base64.getUrlDecoder().decode(segments[1]));
        assertEquals("HS256", header.get("alg").asText());
        assertEquals("student", claims.get("sub").asText());
        assertEquals("STUDENT", claims.get("role").asText());
        assertEquals("angular-workshop", claims.get("client").asText());
        assertEquals(LegacyTokenUtil.ISSUER, claims.get("iss").asText());
        assertEquals(LegacyTokenUtil.AUDIENCE, claims.get("aud").asText());
        assertEquals(LegacyTokenUtil.TOKEN_TTL_SECONDS, claims.get("exp").asLong() - claims.get("iat").asLong());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedSignature = mac.doFinal((segments[0] + "." + segments[1]).getBytes(StandardCharsets.US_ASCII));
        assertEquals(segments[2], Base64.getUrlEncoder().withoutPadding().encodeToString(expectedSignature));
    }

    @Test
    void rejectsMissingOrMalformedClient() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then().statusCode(400);

        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"bad client\"}")
                .when().post("/auth/login")
                .then().statusCode(400);
    }

    @Test
    void treatsSqlSpecialCharactersAsCredentialValues() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"'\",\"password\":\"'\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then().statusCode(401);
    }
}
