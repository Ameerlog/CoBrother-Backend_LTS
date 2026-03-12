package com.cobrother.web.service.auth;

import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Generate and send OTP for login
     */
    public String sendOtpForLogin(String toEmail) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Login OTP");

            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #333;'>Login Verification</h2>" +
                    "<p>Your OTP for login is:</p>" +
                    "<div style='background-color: #f4f4f4; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; margin: 20px 0;'>" +
                    otp +
                    "</div>" +
                    "<p style='color: #666;'>This OTP will expire in 5 minutes.</p>" +
                    "<p style='color: #666;'>If you didn't request this OTP, please ignore this email.</p>" +
                    "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

            return otp;
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage());
        }
    }

    /**
     * Send verification email for new registration
     */
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Your Email Address");

            String verificationUrl = baseUrl + "/api/v1/auth/verify-email?token=" + verificationToken;

            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #333;'>Welcome! Please Verify Your Email</h2>" +
                    "<p>Thank you for registering. Please click the button below to verify your email address:</p>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + verificationUrl + "' style='background-color: #4CAF50; color: white; padding: 14px 28px; text-decoration: none; border-radius: 4px; display: inline-block;'>Verify Email</a>" +
                    "</div>" +
                    "<p style='color: #666;'>Or copy and paste this link in your browser:</p>" +
                    "<p style='word-break: break-all; color: #0066cc;'>" + verificationUrl + "</p>" +
                    "<p style='color: #666; margin-top: 30px;'>This verification link will expire in 24 hours.</p>" +
                    "<p style='color: #666;'>If you didn't create an account, please ignore this email.</p>" +
                    "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    /**
     * Verify OTP code
     */
    public boolean verifyOtp(String email, String otpCode) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtp() == null || user.getOtpExpiry() == null) {
            return false;
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        return user.getOtp().equals(otpCode);
    }

    /**
     * Resend OTP
     */
    public String resendOtp(String email) {
        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmailVerified()) {
            throw new RuntimeException("Please verify your email first before using OTP login");
        }

        String otp = sendOtpForLogin(email);
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        return otp;
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Your Password");

            String resetUrl = baseUrl + "/api/v1/auth/reset-password?token=" + resetToken;

            String htmlContent = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #333;'>Password Reset Request</h2>" +
                    "<p>We received a request to reset your password. Click the button below to reset it:</p>" +
                    "<div style='text-align: center; margin: 30px 0;'>" +
                    "<a href='" + resetUrl + "' style='background-color: #2196F3; color: white; padding: 14px 28px; text-decoration: none; border-radius: 4px; display: inline-block;'>Reset Password</a>" +
                    "</div>" +
                    "<p style='color: #666;'>Or copy and paste this link in your browser:</p>" +
                    "<p style='word-break: break-all; color: #0066cc;'>" + resetUrl + "</p>" +
                    "<p style='color: #666; margin-top: 30px;'>This link will expire in 1 hour.</p>" +
                    "<p style='color: #666;'>If you didn't request a password reset, please ignore this email.</p>" +
                    "</div></body></html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage());
        }
    }

    @Async
    public void sendDomainPurchaseBuyerEmail(String toEmail, String buyerName,
                                             String domainFullName, double amount,
                                             String paymentId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Domain Purchase Confirmed — " + domainFullName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>Domain Purchase Confirmed!</h2>" +
                    "<p>Hi " + buyerName + ",</p>" +
                    "<p>Your purchase of <strong>" + domainFullName + "</strong> was successful.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Domain:</strong> " + domainFullName + "</p>" +
                    "<p><strong>Amount Paid:</strong> ₹" + String.format("%.2f", amount) + "</p>" +
                    "<p><strong>Payment ID:</strong> " + paymentId + "</p>" +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>⏳ You will be updated within 24 hours with domain transfer details.</p>" +
                    "<p style='color:#666;'>If you have any questions, please reply to this email.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send buyer email: " + e.getMessage());
        }
    }

    @Async
    public void sendDomainPurchaseSellerEmail(String toEmail, String sellerName,
                                              String domainFullName, String buyerName,
                                              double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Domain Was Purchased — " + domainFullName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>Your Domain Has Been Sold! 🎉</h2>" +
                    "<p>Hi " + sellerName + ",</p>" +
                    "<p><strong>" + domainFullName + "</strong> was purchased by <strong>" + buyerName + "</strong>.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Domain:</strong> " + domainFullName + "</p>" +
                    "<p><strong>Sale Amount:</strong> ₹" + String.format("%.2f", amount) + "</p>" +
                    "<p><strong>Buyer:</strong> " + buyerName + "</p>" +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>Please initiate the domain transfer within 24 hours.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send seller email: " + e.getMessage());
        }
    }
}