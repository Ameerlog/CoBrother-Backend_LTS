package com.cobrother.web.service.feedback;

import com.cobrother.web.Entity.feedback.Feedback;
import com.cobrother.web.Entity.feedback.FeedbackRequest;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.FeedbackRepository;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.auth.FeedbackMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FeedbackService {

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private FeedbackMailService feedbackMailService;

    public ResponseEntity<Map<String, String>> submitFeedback(FeedbackRequest request) {
        AppUser user = null;
        try {
            user = currentUserService.getCurrentUser();
        } catch (Exception e) {
        }

        Feedback feedback = new Feedback(
                request.getFeedbackType(),
                request.getMessage(),
                request.getPageUrl(),
                user
        );

        feedbackRepository.save(feedback);

        if ("like".equalsIgnoreCase(request.getFeedbackType())) {
            feedbackMailService.sendFeedbackLikeEmail(
                    user != null ? user.getEmail() : "anonymous",
                    user != null ? user.getFirstname() + " " + user.getLastname() : "Anonymous User",
                    request.getPageUrl(),
                    request.getMessage()
            );
        } else if ("dislike".equalsIgnoreCase(request.getFeedbackType())) {
            feedbackMailService.sendFeedbackDislikeEmail(
                    user != null ? user.getEmail() : "anonymous",
                    user != null ? user.getFirstname() + " " + user.getLastname() : "Anonymous User",
                    request.getPageUrl(),
                    request.getMessage()
            );
        }

        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
