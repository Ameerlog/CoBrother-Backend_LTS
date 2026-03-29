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

    @Async
    public void sendSoftwarePurchaseBuyerEmail(String toEmail, String buyerName,
                                               String softwareName, double amount,
                                               String paymentId, String githubLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Software Purchase Confirmed — " + softwareName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>Purchase Confirmed!</h2>" +
                    "<p>Hi " + buyerName + ",</p>" +
                    "<p>Your purchase of <strong>" + softwareName + "</strong> was successful.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Software:</strong> " + softwareName + "</p>" +
                    "<p><strong>Amount Paid:</strong> ₹" + String.format("%.2f", amount) + "</p>" +
                    "<p><strong>Payment ID:</strong> " + paymentId + "</p>" +
                    "</div>" +
                    "<p><strong>GitHub Repository:</strong></p>" +
                    "<div style='background:#f4f4f4; padding:12px; border-radius:8px; margin:10px 0;'>" +
                    "<a href='" + githubLink + "' style='color:#0066cc;'>" + githubLink + "</a>" +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>Once you've verified everything works, " +
                    "please mark the purchase as complete from your dashboard.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send buyer email: " + e.getMessage());
        }
    }

    @Async
    public void sendSoftwarePurchaseSellerEmail(String toEmail, String sellerName,
                                                String softwareName, String buyerName,
                                                double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Software Was Purchased — " + softwareName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>Your Software Has Been Sold! 🎉</h2>" +
                    "<p>Hi " + sellerName + ",</p>" +
                    "<p><strong>" + softwareName + "</strong> was purchased by <strong>" + buyerName + "</strong>.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Software:</strong> " + softwareName + "</p>" +
                    "<p><strong>Sale Amount:</strong> ₹" + String.format("%.2f", amount) + "</p>" +
                    "<p><strong>Buyer:</strong> " + buyerName + "</p>" +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>The GitHub link will be shared with the buyer " +
                    "once they confirm everything is working.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send seller email: " + e.getMessage());
        }
    }

    @Async
    public void sendGithubLinkEmail(String toEmail, String buyerName,
                                    String softwareName, String githubLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your GitHub Access — " + softwareName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>GitHub Repository Access</h2>" +
                    "<p>Hi " + buyerName + ",</p>" +
                    "<p>Thank you for confirming your purchase of <strong>" + softwareName + "</strong>.</p>" +
                    "<p>Here is your GitHub repository link:</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0; text-align:center;'>" +
                    "<a href='" + githubLink + "' style='color:#0066cc; font-weight:bold; font-size:1.1rem;'>" +
                    githubLink + "</a>" +
                    "</div>" +
                    "<p style='color:#666;'>Keep this link safe. If you have any issues, reply to this email.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send GitHub link email: " + e.getMessage());
        }
    }

    @Async
    public void sendDomainVerificationEmail(String toEmail, String domainName, String token) {
        try {
            // Last 6 chars of token used as the OTP code
            String code = token.substring(token.length() - 6).toUpperCase();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify Domain Ownership — " + domainName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>Domain Ownership Verification</h2>" +
                    "<p>A request was made to verify ownership of <strong>" + domainName + "</strong> on CoBrother.</p>" +
                    "<p>Your verification code is:</p>" +
                    "<div style='background:#f4f4f4; padding:20px; text-align:center; " +
                    "font-size:2rem; font-weight:bold; letter-spacing:8px; " +
                    "border-radius:8px; margin:20px 0;'>" + code + "</div>" +
                    "<p style='color:#666;'>This code expires in 30 minutes.</p>" +
                    "<p style='color:#666;'>If you did not request this, please ignore this email.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage());
        }
    }

    @Async
    public void sendCoBrotherFeeRequestEmail(String toEmail, String listerName,
                                             String entityTitle, Long requestId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Action Required: CoBrother Service Fee — " + entityTitle);

            String paymentUrl = "http://localhost:3000/fee-requests"; // frontend route

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>CoBrother Service Request</h2>" +
                    "<p>Hi " + listerName + ",</p>" +
                    "<p>A CoBrother has been assigned to assist with your listing: " +
                    "<strong>" + entityTitle + "</strong></p>" +
                    "<p>To proceed, a one-time service fee of <strong>₹1,000</strong> is required.</p>" +
                    "<div style='text-align:center; margin: 30px 0;'>" +
                    "<a href='" + paymentUrl + "' style='background:#c8a96e; color:#1a1a2e; " +
                    "padding:14px 28px; text-decoration:none; border-radius:6px; " +
                    "font-weight:bold; display:inline-block;'>Pay ₹1,000 Now</a>" +
                    "</div>" +
                    "<p style='color:#666;'>You can also cancel this request if you wish.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send fee request email: " + e.getMessage());
        }
    }

    @Async
    public void sendCoBrotherAssignmentEmail(String toEmail, String coBrotherName,
                                             com.cobrother.web.Entity.cobrother.CoBrotherRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("New Request Assigned — " + request.getEntityTitle());

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>New CoBrother Request</h2>" +
                    "<p>Hi " + coBrotherName + ",</p>" +
                    "<p>You have been assigned a new request. Details below:</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Type:</strong> " + request.getRequestType().name() + "</p>" +
                    "<p><strong>Entity:</strong> " + request.getEntityTitle() + "</p>" +
                    "<p><strong>Lister Name:</strong> " + request.getListerName() + "</p>" +
                    "<p><strong>Lister Email:</strong> " + request.getListerEmail() + "</p>" +
                    "<p><strong>Lister Phone:</strong> " + safeStr(request.getListerPhone()) + "</p>" +
                    "<p><strong>Applicant/Buyer:</strong> " + safeStr(request.getApplicantName()) + "</p>" +
                    "<p><strong>Applicant Email:</strong> " + safeStr(request.getApplicantEmail()) + "</p>" +
                    "<p><strong>Applicant Phone:</strong> " + safeStr(request.getApplicantPhone()) + "</p>" +
                    "</div>" +
                    "<p>Please log in to your CoBrother dashboard to accept or reject this request.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send assignment email: " + e.getMessage());
        }
    }

    @Async
    public void sendCoBrotherResponseEmail(String toEmail, String listerName,
                                           String entityTitle, boolean accepted, String note) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("CoBrother Request " + (accepted ? "Accepted" : "Rejected") +
                    " — " + entityTitle);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>CoBrother Request Update</h2>" +
                    "<p>Hi " + listerName + ",</p>" +
                    "<p>Your CoBrother request for <strong>" + entityTitle + "</strong> has been " +
                    "<strong style='color:" + (accepted ? "#27ae60" : "#e74c3c") + ";'>" +
                    (accepted ? "accepted ✓" : "rejected ✕") + "</strong>.</p>" +
                    (note != null && !note.isBlank()
                            ? "<p><strong>Note:</strong> " + note + "</p>"
                            : "") +
                    "<p style='color:#666;'>Please log in to your dashboard for more details.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send response email: " + e.getMessage());
        }
    }

    private String safeStr(String s) { return s != null ? s : "—"; }

    @Async
    public void sendAuctionWinnerEmail(String toEmail, String winnerName,
                                       String domainName, double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🎉 You Won the Auction — " + domainName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>Congratulations! You Won 🎉</h2>" +
                    "<p>Hi " + winnerName + ",</p>" +
                    "<p>You won the auction for <strong>" + domainName + "</strong>.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Domain:</strong> " + domainName + "</p>" +
                    "<p><strong>Winning Bid:</strong> ₹" + String.format("%.2f", amount) + "</p>" +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>Our team will contact you shortly " +
                    "to coordinate the domain transfer and payment.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Async
    public void sendAuctionEndedSellerEmail(String toEmail, String sellerName,
                                            String domainName, String winnerName,
                                            double amount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Auction Ended — " + domainName);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>Auction Completed</h2>" +
                    "<p>Hi " + sellerName + ",</p>" +
                    "<p>Your auction for <strong>" + domainName + "</strong> has ended.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Winner:</strong> " + winnerName + "</p>" +
                    "<p><strong>Winning Bid:</strong> ₹" + String.format("%.2f", amount) + "</p>" +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>Our admin team will coordinate " +
                    "the transfer with both parties.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}