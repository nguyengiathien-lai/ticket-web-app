package com.ticketapp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.entity.Account;
import com.ticketapp.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret:change-this-local-dev-secret-that-is-at-least-32-bytes}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }

        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(Account account) {
        Instant now = Instant.now();
        Set<String> roles = account.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", account.getId());
        claims.put("email", account.getEmail());
        claims.put("roles", roles);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(claims);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public boolean isTokenValid(String token) {
        try {
            Map<String, Object> claims = extractClaims(token);
            Object expiration = claims.get("exp");

            return toLong(expiration) > Instant.now().getEpochSecond();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public String extractAccountId(String token) {
        Object subject = extractClaims(token).get("sub");
        if (subject == null || subject.toString().isBlank()) {
            throw new IllegalArgumentException("Token subject is missing");
        }

        return subject.toString();
    }

    public long extractExpirationEpochSeconds(String token) {
        return toLong(extractClaims(token).get("exp"));
    }

    private Map<String, Object> extractClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Token is malformed");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("Token signature is invalid");
        }

        try {
            byte[] claimsJson = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(claimsJson, CLAIMS_TYPE);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Token claims are invalid", exception);
        }
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize JWT content", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text) {
            return Long.parseLong(text);
        }

        throw new IllegalArgumentException("Token expiration is invalid");
    }
}
