package com.campuslands.ops.security;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Implementacion estandar RFC 7519 de JSON Web Token (JWT) con firma HMAC-SHA256 (HS256)
 * usando criptografia nativa de Java (javax.crypto.Mac) para evitar errores de librerias externas en el IDE.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret:campuslands-ops-local-dev-only-change-me}")
    private String secret;

    @Value("${jwt.expiration:7200000}")
    private long expirationMs;

    @Value("${jwt.issuer:CampuslandOps.Api}")
    private String issuer;

    @Value("${jwt.audience:CampuslandOps.Client}")
    private String audience;

    private final Gson gson = new Gson();

    // Genera un token JWT estandar de 3 partes: Header.Payload.Signature
    public String generateToken(Long userId, String fullName, List<String> roles) {
        long nowSec = System.currentTimeMillis() / 1000;
        long expSec = (System.currentTimeMillis() + expirationMs) / 1000;

        String headerJson = gson.toJson(Map.of(
                "alg", "HS256",
                "typ", "JWT"
        ));

        String payloadJson = gson.toJson(Map.of(
                "sub", String.valueOf(userId),
                "iss", issuer,
                "aud", audience,
                "iat", nowSec,
                "exp", expSec,
                "fullName", fullName,
                "roles", roles
        ));

        String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String contentToSign = encodedHeader + "." + encodedPayload;
        String signature = signHmacSha256(contentToSign);

        return contentToSign + "." + signature;
    }

    // Valida firma, issuer, audience y expiracion, retornando los Claims en JsonObject
    public JsonObject validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token vacio");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Estructura JWT invalida");
        }

        String content = parts[0] + "." + parts[1];
        String expectedSig = signHmacSha256(content);
        if (!expectedSig.equals(parts[2])) {
            throw new IllegalArgumentException("Firma digital JWT invalida");
        }

        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonObject claims = JsonParser.parseString(payloadJson).getAsJsonObject();

        long nowSec = System.currentTimeMillis() / 1000;
        if (claims.has("exp") && claims.get("exp").getAsLong() < nowSec) {
            throw new IllegalArgumentException("Token JWT expirado");
        }
        if (!issuer.equals(claims.get("iss").getAsString()) || !audience.equals(claims.get("aud").getAsString())) {
            throw new IllegalArgumentException("Issuer o Audience invalidos en JWT");
        }
        return claims;
    }

    public Long getUserId(String token) {
        return Long.parseLong(validateToken(token).get("sub").getAsString());
    }

    public List<String> getRoles(String token) {
        JsonArray arr = validateToken(token).getAsJsonArray("roles");
        List<String> list = new ArrayList<>();
        arr.forEach(el -> list.add(el.getAsString()));
        return list;
    }

    public boolean isTokenExpired(String token) {
        try {
            validateToken(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String signHmacSha256(String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] sigBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(sigBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error firmando JWT con HmacSHA256", e);
        }
    }
}
