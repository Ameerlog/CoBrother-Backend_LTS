package com.cobrother.web.controller;

import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.RefreshToken;
import com.cobrother.web.model.JwtResponseDto;
import com.cobrother.web.model.RefreshTokenRequestDto;
import com.cobrother.web.service.auth.JwtService;
import com.cobrother.web.service.auth.RefreshTokenService;
import com.cobrother.web.service.auth.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class TokenController {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    /**
     * Refresh access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDto refreshTokenRequestDto) {
        try {
            return refreshTokenService.findByToken(refreshTokenRequestDto.getRefreshToken())
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        // Generate new access token
                        Map<String, Object> claims = new HashMap<>();
                        claims.put("userId", user.getId());
                        claims.put("role", user.getRole().name());
                        String accessToken = jwtService.generateToken(user.getEmail(), claims);

                        JwtResponseDto jwtResponseDto = new JwtResponseDto();

                        jwtResponseDto.setAccessToken(accessToken);
                        jwtResponseDto.setRefreshToken(refreshTokenRequestDto.getRefreshToken());
                        jwtResponseDto.setUserId(user.getId());
                        jwtResponseDto.setEmail(user.getEmail());
                        jwtResponseDto.setRole(user.getRole());
                        jwtResponseDto.setExpiresIn(jwtService.getExpirationTime());
                        jwtResponseDto.setNewUser(false);
                        jwtResponseDto.setEmailVerified(user.getEmailVerified());

                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Token refreshed successfully");
                        response.put("data", jwtResponseDto);

                        return ResponseEntity.ok(response);
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("success", false, "error", "Invalid refresh token"))
                    );
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "An error occurred while refreshing token")
            );
        }
    }

    /**
     * Logout user by revoking refresh token
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractUsername(token);
            AppUser user = userDetailsService.getUserByEmail(email);

            // Revoke all refresh tokens for this user
            refreshTokenService.revokeUserTokens(user);

            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "An error occurred during logout")
            );
        }
    }
}