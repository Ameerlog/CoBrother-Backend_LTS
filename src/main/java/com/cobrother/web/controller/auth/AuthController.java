package com.cobrother.web.controller.auth;

import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.model.auth.*;
import com.cobrother.web.service.auth.AuthService;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.auth.JwtService;
import com.cobrother.web.service.auth.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private CurrentUserService currentUserService;
    // ==================== REGISTRATION & EMAIL VERIFICATION ====================

    /**
     * Register new user with email and password
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<?> register( @RequestBody RegisterRequestDto registerRequest) {
        return authService.registerUser(registerRequest);
    }

    /**
     * Verify email using token sent to user's email
     * GET /api/v1/auth/verify-email?token=xxx
     */
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
        return authService.verifyEmail(token);
    }

    /**
     * Resend verification email
     * POST /api/v1/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "error", "Email is required")
            );
        }
        return authService.resendVerificationEmail(email);
    }

    // ==================== PASSWORD-BASED LOGIN ====================

    /**
     * Login with email and password
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login( @RequestBody LoginRequestDto loginRequest) {
        return authService.loginWithPassword(loginRequest);
    }

    // ==================== OTP-BASED LOGIN ====================

    /**
     * Send OTP to email for login (only for verified users)
     * POST /api/v1/auth/otp/send
     */
    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestBody OtpRequestDto otpRequest) {
        return authService.sendOtpForLogin(otpRequest);
    }

    /**
     * Verify OTP and login
     * POST /api/v1/auth/otp/verify
     */
    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp( @RequestBody OtpVerifyDto otpVerify) {
        return authService.verifyOtpAndLogin(otpVerify);
    }

    /**
     * Resend OTP for login
     * POST /api/v1/auth/otp/resend
     */
    @PostMapping("/otp/resend")
    public ResponseEntity<?> resendOtp( @RequestBody OtpRequestDto otpRequest) {
        return authService.sendOtpForLogin(otpRequest);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractUsername(token);
            AppUser user = userDetailsService.getUserByEmail(email);

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            userInfo.put("emailVerified", user.getEmailVerified());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", userInfo);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "error", "Invalid or expired token")
            );
        }
    }

    /**
     * Update user profile
     * PUT /api/v1/auth/profile/update
     */
    @PutMapping("/profile/update")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody UserInfoDto userInfoDto
    ) {
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractUsername(token);

            AppUser updatedUser = userDetailsService.updateUserProfile(email, userInfoDto);

            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", updatedUser.getId());
            userMap.put("email", updatedUser.getEmail());
            userMap.put("role", updatedUser.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("user", userMap);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Failed to update profile")
            );
        }
    }

    @GetMapping("/mee")
    public ResponseEntity<?> getCurrentUser() {

        AppUser user = currentUserService.getCurrentUser();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        userInfo.put("emailVerified", user.getEmailVerified());

        return ResponseEntity.ok(userInfo);
    }
}