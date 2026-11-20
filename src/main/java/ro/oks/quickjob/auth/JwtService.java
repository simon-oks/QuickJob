package ro.oks.quickjob.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.oks.quickjob.auth.dto.AuthResponse;
import ro.oks.quickjob.common.error.BusinessException;
import ro.oks.quickjob.common.error.ErrorCode;
import ro.oks.quickjob.user.User;
import org.springframework.http.HttpStatus;

@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokenRepository;

    public JwtService(JwtProperties properties, RefreshTokenRepository refreshTokenRepository) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes());
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String generateAccessToken(UUID userId, String roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTokenExpirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    @Transactional
    public AuthResponse generateTokenPair(User user) {
        String accessToken = generateAccessToken(user.getId(), user.getRoles());
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawRefreshToken);

        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusDays(properties.refreshTokenExpirationDays());

        RefreshToken refreshToken = new RefreshToken(UUID.randomUUID(), user.getId(), tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, properties.accessTokenExpirationMinutes() * 60);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken, User user) {
        String tokenHash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AUTH_INVALID_TOKEN,
                        "auth.invalid_token",
                        HttpStatus.UNAUTHORIZED));

        if (!stored.isUsable()) {
            throw new BusinessException(
                    ErrorCode.AUTH_INVALID_TOKEN,
                    "auth.token_expired",
                    HttpStatus.UNAUTHORIZED);
        }

        // Rotation : révoquer l'ancien, générer un nouveau
        stored.revoke();

        String newRawRefreshToken = UUID.randomUUID().toString();
        String newTokenHash = sha256(newRawRefreshToken);
        OffsetDateTime expiresAt = OffsetDateTime.now()
                .plusDays(properties.refreshTokenExpirationDays());

        RefreshToken newRefreshToken = new RefreshToken(UUID.randomUUID(), user.getId(), newTokenHash, expiresAt);
        refreshTokenRepository.save(newRefreshToken);

        String accessToken = generateAccessToken(user.getId(), user.getRoles());
        return new AuthResponse(accessToken, newRawRefreshToken, properties.accessTokenExpirationMinutes() * 60);
    }

    @Transactional
    public void revokeAllTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    public UUID getUserIdFromRefreshToken(String rawRefreshToken) {
        String tokenHash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AUTH_INVALID_TOKEN,
                        "auth.invalid_token",
                        HttpStatus.UNAUTHORIZED));
        return stored.getUserId();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            return null;
        }
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}
