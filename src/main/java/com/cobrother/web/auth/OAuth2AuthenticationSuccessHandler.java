package com.cobrother.web.auth;

import com.cobrother.web.Entity.user.RefreshToken;
import com.cobrother.web.service.auth.CustomUserDetails;
import com.cobrother.web.service.auth.JwtService;
import com.cobrother.web.service.auth.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Runs after a successful Google OAuth2 login.
 * Issues a JWT + refresh token and redirects to the frontend
 * with tokens as query params so the SPA can store them.
 *
 * FIX 1: Corrected @Value annotation — was missing closing brace:
 *   BROKEN:  @Value("${app.oauth2.redirect-uri")
 *   FIXED:   @Value("${app.oauth2.redirect-uri}")
 *
 * FIX 2: Added profileComplete param to redirect URL so the
 *   frontend can immediately route to /complete-profile if needed.
 *
 * application.properties:
 *   app.oauth2.redirect-uri=http://localhost:3000/auth/callback
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // ✅ FIX: was @Value("${app.oauth2.redirect-uri")  ← missing closing }
    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Build JWT claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userDetails.getUserId());
        claims.put("profileComplete", userDetails.isProfileComplete());
        userDetails.getAuthorities().stream()
                .findFirst()
                .ifPresent(a -> claims.put("role", a.getAuthority()));

        String jwt = jwtService.generateToken(userDetails.getUsername(), claims);

        // Create / rotate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        // ✅ FIX: Added profileComplete to redirect so frontend can route correctly
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", jwt)
                .queryParam("refreshToken", refreshToken.getToken())
                .queryParam("isNewUser", userDetails.isNewUser())
                .queryParam("profileComplete", userDetails.isProfileComplete())
                .queryParam("expiresIn", jwtService.getExpirationTime())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}