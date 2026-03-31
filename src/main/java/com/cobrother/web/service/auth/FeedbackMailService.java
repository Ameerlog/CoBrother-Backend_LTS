package com.cobrother.web.service.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FeedbackMailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendFeedbackLikeEmail(String userEmail, String userName,
                                      String pageUrl, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(fromEmail);
            helper.setSubject("👍 Positive Feedback Received");

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #27ae60;'>👍 User Liked Your Platform!</h2>" +
                    "<p>Great news! A user has given positive feedback.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>User:</strong> " + userName + "</p>" +
                    "<p><strong>Email:</strong> " + userEmail + "</p>" +
                    "<p><strong>Page URL:</strong> " + pageUrl + "</p>" +
                    "<p><strong>Feedback Type:</strong> <span style='color:#27ae60; font-weight:bold;'>LIKE 👍</span></p>" +
                    (message != null && !message.isBlank()
                            ? "<p><strong>Message:</strong> " + message + "</p>"
                            : "") +
                    "</div>" +
                    "<p style='color:#666;'>Keep up the great work!</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send feedback like email: " + e.getMessage());
        }
    }

    @Async
    public void sendFeedbackDislikeEmail(String userEmail, String userName,
                                         String pageUrl, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(fromEmail);
            helper.setSubject("👎 Negative Feedback Received - Action Required");

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #e74c3c;'>👎 User Disliked Something</h2>" +
                    "<p>A user has given negative feedback. Please review and take action.</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>User:</strong> " + userName + "</p>" +
                    "<p><strong>Email:</strong> " + userEmail + "</p>" +
                    "<p><strong>Page URL:</strong> " + pageUrl + "</p>" +
                    "<p><strong>Feedback Type:</strong> <span style='color:#e74c3c; font-weight:bold;'>DISLIKE 👎</span></p>" +
                    (message != null && !message.isBlank()
                            ? "<p><strong>Message:</strong> " + message + "</p>"
                            : "") +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>⚠️ Please investigate and address this feedback.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send feedback dislike email: " + e.getMessage());
        }
    }

}
