package com.cobrother.web.service.auth;

import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.RefreshToken;
import com.cobrother.web.Entity.user.UserRole;
import com.cobrother.web.Repository.UserRepository;
import com.cobrother.web.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * Register new user with email and password
     */
    @Transactional
    public ResponseEntity<?> registerUser(RegisterRequestDto registerRequest) {
        try {
            // Validate email format
            String email = registerRequest.getEmail().toLowerCase().trim();

            // Check if user already exists
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        Map.of("success", false, "error", "Email already registered")
                );
            }

            // Create new user
            AppUser user = new AppUser();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole(UserRole.USER);
            user.setActive(true);
            user.setEmailVerified(false);

            // Generate verification token
            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));

            // Save user
            user = userRepository.save(user);

            // Send verification email
            mailService.sendVerificationEmail(email, verificationToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful. Please check your email to verify your account.");
            response.put("userId", user.getId());
            response.put("email", user.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Registration failed: " + e.getMessage())
            );
        }
    }

    /**
     * Verify email using verification token
     */
    @Transactional
    public ResponseEntity<?> verifyEmail(String token) {
        try {
            AppUser user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new RuntimeException("Invalid verification token"));

            if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of("success", false, "error", "Verification token has expired")
                );
            }

            user.setEmailVerified(true);
            user.setVerificationToken(null);
            user.setVerificationTokenExpiry(null);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Email verified successfully. You can now login."
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("success", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Email verification failed")
            );
        }
    }

    /**
     * Resend verification email
     */
    @Transactional
    public ResponseEntity<?> resendVerificationEmail(String email) {
        try {
            AppUser user = userRepository.findByEmail(email.toLowerCase().trim())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getEmailVerified()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                        Map.of("success", false, "error", "Email is already verified")
                );
            }

            // Generate new verification token
            String verificationToken = UUID.randomUUID().toString();
            user.setVerificationToken(verificationToken);
            user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
            userRepository.save(user);

            // Send verification email
            mailService.sendVerificationEmail(email, verificationToken);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Verification email sent successfully"
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("success", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Failed to resend verification email")
            );
        }
    }

    /**
     * Login with email and password
     */
    @Transactional
    public ResponseEntity<?> loginWithPassword(LoginRequestDto loginRequest) {
        try {
            String email = loginRequest.getEmail().toLowerCase().trim();

            // Check if user exists
            AppUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            // Check if email is verified
            if (!user.getEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "error", "Please verify your email before logging in",
                                "emailVerified", false)
                );
            }

            // Check if account is active
            if (!user.getActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "error", "Account is deactivated")
                );
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword())
            );

            if (!authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "error", "Invalid email or password")
                );
            }

            // Generate tokens and return response
            return generateAuthResponse(user);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Login failed: " + e.getMessage())
            );
        }
    }

    /**
     * Send OTP for login (only for verified users)
     */
    @Transactional
    public ResponseEntity<?> sendOtpForLogin(OtpRequestDto otpRequest) {
        try {
            String email = otpRequest.getEmail().toLowerCase().trim();

            // Validate email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return ResponseEntity.badRequest().body(
                        Map.of("success", false, "error", "Invalid email format")
                );
            }

            // Check if user exists
            AppUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found. Please register first."));

            // Check if email is verified
            if (!user.getEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "error", "Please verify your email first",
                                "emailVerified", false)
                );
            }

            // Check if account is active
            if (!user.getActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "error", "Account is deactivated")
                );
            }

            // Generate and send OTP
            String otp = mailService.sendOtpForLogin(email);
            user.setOtp(otp);
            user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "OTP sent successfully to " + email,
                    "email", email
            ));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of("success", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Failed to send OTP: " + e.getMessage())
            );
        }
    }

    /**
     * Verify OTP and login
     */
    @Transactional
    public ResponseEntity<?> verifyOtpAndLogin(OtpVerifyDto otpVerify) {
        try {
            String email = otpVerify.getEmail().toLowerCase().trim();

            // Get user
            AppUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if email is verified
            if (!user.getEmailVerified()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        Map.of("success", false, "error", "Email not verified")
                );
            }

            // Verify OTP
            boolean isVerified = mailService.verifyOtp(email, otpVerify.getOtpCode());

            if (!isVerified) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                        Map.of("success", false, "error", "Invalid or expired OTP")
                );
            }

            // Clear OTP after successful verification
            user.setOtp(null);
            user.setOtpExpiry(null);
            userRepository.save(user);

            // Generate tokens and return response
            return generateAuthResponse(user);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("success", false, "error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "OTP verification failed: " + e.getMessage())
            );
        }
    }

    /**
     * Generate authentication response with tokens
     */
    private ResponseEntity<?> generateAuthResponse(AppUser user) {
        // Create refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        // Generate JWT token with claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        String jwtToken = jwtService.generateToken(user.getEmail(), claims);

        // Build response
        JwtResponseDto jwtResponse = new JwtResponseDto();
        jwtResponse.setAccessToken(jwtToken);
        jwtResponse.setRefreshToken(refreshToken.getToken());
        jwtResponse.setUserId(user.getId());
        jwtResponse.setEmail(user.getEmail());
        jwtResponse.setRole(user.getRole());
        jwtResponse.setExpiresIn(jwtService.getExpirationTime());
        jwtResponse.setNewUser(false);
        jwtResponse.setEmailVerified(user.getEmailVerified());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Login successful");
        response.put("data", jwtResponse);

        return ResponseEntity.ok(response);
    }
}