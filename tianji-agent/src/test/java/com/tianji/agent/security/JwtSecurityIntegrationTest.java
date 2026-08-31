package com.tianji.agent.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "agent.security.jwt-enabled=true",
        "agent.limits.enabled=false",
        "agent.knowledge.mq-enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class JwtSecurityIntegrationTest {
    private static final RSAKey KEY;
    private static final MockWebServer JWKS = new MockWebServer();

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("tj-auth-rsa").generate();
            JWKS.setDispatcher(new Dispatcher() {
                @Override public MockResponse dispatch(RecordedRequest request) {
                    return new MockResponse().setHeader("Content-Type", "application/json")
                            .setBody("{\"keys\":[" + KEY.toPublicJWK() + "]}");
                }
            });
            JWKS.start();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("agent.security.jwk-set-uri", () -> JWKS.url("/jwks/set").toString());
    }

    @Autowired
    WebTestClient client;

    @AfterAll
    static void closeServer() throws IOException { JWKS.close(); }

    @Test
    void rejectsMissingToken() {
        client.post().uri("/api/v1/conversations").contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\":\"x\",\"scene\":\"CHAT\"}")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void verifiedJwtOverridesSpoofedHeaderAndKeepsConversationOwnership() throws Exception {
        String owner = token(7L, 2L);
        String other = token(8L, 2L);
        client.post().uri("/api/v1/conversations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner)
                .header("user-info", "999")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"title\":\"owner\",\"scene\":\"CHAT\"}")
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.data.title").isEqualTo("owner");

        client.get().uri("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + owner)
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.data.length()").isEqualTo(1);
        client.get().uri("/api/v1/conversations").header(HttpHeaders.AUTHORIZATION, "Bearer " + other)
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.data.length()").isEqualTo(0);
    }

    private String token(Long userId, Long roleId) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("user", Map.of("userId", userId, "roleId", roleId)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT).keyID("tj-auth-rsa").build(), claims);
        jwt.sign(new RSASSASigner(KEY));
        return jwt.serialize();
    }
}
