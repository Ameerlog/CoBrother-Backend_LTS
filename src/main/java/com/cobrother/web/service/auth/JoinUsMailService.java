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
public class JoinUsMailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendJoinUsEmail(String name, String email, String phone, String skill, String city, String equipment) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(fromEmail);
            helper.setSubject("New CoBrother Application - " + name);

            String html = "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                    "<h2 style='color: #1a1a2e;'>🎯 New CoBrother Application</h2>" +
                    "<p>A new application has been submitted to join the CoBrother team!</p>" +
                    "<div style='background:#f4f4f4; padding:15px; border-radius:8px; margin:20px 0;'>" +
                    "<p><strong>Full Name:</strong> " + name + "</p>" +
                    "<p><strong>Email:</strong> " + email + "</p>" +
                    "<p><strong>Phone/WhatsApp:</strong> " + phone + "</p>" +
                    "<p><strong>Top Skill:</strong> " + skill + "</p>" +
                    "<p><strong>City/Pincode:</strong> " + city + "</p>" +
                    "<p><strong>Equipment Availability:</strong> " + equipment + "</p>" +
                    "</div>" +
                    "<p style='color:#e67e22; font-weight:600;'>Please review this application and contact the candidate.</p>" +
                    "</div></body></html>";

            helper.setText(html, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send join-us email: " + e.getMessage());
        }
    }
}
