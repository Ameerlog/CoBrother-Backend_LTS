package com.cobrother.web.service.auth;

import com.cobrother.web.Entity.AppUser;
import com.cobrother.web.Entity.RefreshToken;
import com.cobrother.web.Repository.RefreshTokenRepository;
import com.cobrother.web.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.refresh.expiration:2592000000}") // Default 30 days in milliseconds
    private Long refreshTokenExpiration;

    @Transactional
    public RefreshToken createRefreshToken(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Revoke any existing refresh tokens for this user
        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .ifPresent(existingToken -> {
                    existingToken.setRevoked(true);
                    refreshTokenRepository.save(existingToken);
                });

        // Create new refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpiration));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getRevoked()) {
            throw new RuntimeException("Refresh token has been revoked. Please log in again.");
        }
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token expired. Please log in again.");
        }
        return token;
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public void revokeUserTokens(AppUser user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void deleteUserTokens(AppUser user) {
        refreshTokenRepository.deleteByUser(user);
    }
}