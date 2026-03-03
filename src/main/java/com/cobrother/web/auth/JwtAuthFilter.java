package com.cobrother.web.auth;

import com.cobrother.web.service.auth.JwtService;
import com.cobrother.web.service.auth.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Add token expiration info to response header
                    Date expiration = jwtService.extractExpiration(jwt);
                    response.setHeader("X-Token-Expiration", String.valueOf(expiration.getTime()));

                    // Warn if token expires soon (e.g., within 5 minutes)
                    long timeUntilExpiry = expiration.getTime() - System.currentTimeMillis();
                    if (timeUntilExpiry < 300000) { // 5 minutes
                        response.setHeader("X-Token-Refresh-Required", "true");
                    }
                }
            }
        } catch (Exception e) {
            // Other JWT errors (invalid signature, malformed, etc.)
            logger.error("JWT authentication error: {}", e);
            response.setHeader("X-Auth-Error", "Invalid token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid token\", \"message\": \"Authentication failed.\"}");
            response.setContentType("application/json");
            return; // Don't continue the filter chain
        }

        filterChain.doFilter(request, response);
    }
}