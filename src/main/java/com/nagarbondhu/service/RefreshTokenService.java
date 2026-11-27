package com.nagarbondhu.service;

import com.nagarbondhu.entity.RefreshToken;
import com.nagarbondhu.entity.User;
import com.nagarbondhu.exception.InvalidTokenException;
import com.nagarbondhu.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public String createRefreshToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Refresh token cannot be blank");
        }

        String tokenHash = hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (!token.getUser().isActive()) {
            throw new InvalidTokenException("User account is deactivated");
        }

        return token;
    }

    @Transactional
    public String rotateRefreshToken(String rawToken) {
        RefreshToken oldToken = verifyRefreshToken(rawToken);
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        return createRefreshToken(oldToken.getUser());
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
