package mr.popo.localaiagent.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    private SecretKey key() {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    public String issueAccessToken(Long userId, String username, List<String> roles) {
        return issue(userId, username, roles, JwtTokenType.ACCESS, properties.getAccessTtl());
    }

    public String issueRefreshToken(Long userId, String username, List<String> roles) {
        return issue(userId, username, roles, JwtTokenType.REFRESH, properties.getRefreshTtl());
    }

    private String issue(Long userId, String username, List<String> roles, JwtTokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .subject(String.valueOf(userId))
                .claims(Map.of(
                        "username", username,
                        "roles", roles,
                        "type", type.name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key(), io.jsonwebtoken.Jwts.SIG.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token);
    }

    public long getAccessTtlSeconds() {
        return properties.getAccessTtl().toSeconds();
    }

    public long getRefreshTtlSeconds() {
        return properties.getRefreshTtl().toSeconds();
    }
}
