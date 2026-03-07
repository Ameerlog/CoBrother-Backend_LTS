package com.cobrother.web.controller.community;

import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.CommunityRepository;
import com.cobrother.web.model.community.CommunityUpdateRequest;
import com.cobrother.web.service.analytics.AnalyticsService;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.community.CommunityService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("api/v1/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CurrentUserService currentUserService;

    @Value("${app.frontend-url:http://localhost:3000}") private String frontendUrl;

    @GetMapping("/linkedin/auth")
    public ResponseEntity<String> getLinkedInAuthUrl() {
        return communityService.getLinkedInAuthUrl();
    }

    // Handles: GET /api/v1/community/linkedin/callback  (Option B path)
    @GetMapping("/linkedin/callback")
    public void linkedInCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,  // JWT token
            HttpServletResponse response) throws IOException {
        doRedirect(code, state, response);
    }

    private void doRedirect(String code, String state, HttpServletResponse response) throws IOException {
        try {
            Long profileId = communityService.handleLinkedInCallbackAndReturnId(code, state);
            String redirectUrl = frontendUrl + "/community?linkedin=success&profileId=" + profileId;
            System.out.println("Redirecting to: " + redirectUrl); // ADD THIS
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            System.out.println("LinkedIn error: " + e.getMessage()); // ADD THIS
            e.printStackTrace(); // ADD THIS
            String err = URLEncoder.encode(
                    e.getMessage() != null ? e.getMessage() : "LinkedIn failed",
                    StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + "/community?linkedin_error=" + err);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody CommunityUpdateRequest req) {
        return communityService.updateCommunityProfile(id, req);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllProfiles() { return communityService.getAllProfiles(); }

    @GetMapping("/my")
    public ResponseEntity<?> getMyProfile() { return communityService.getMyProfile(); }


    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return communityRepository.findById(id)
                .<ResponseEntity<?>>map(profile -> {
                    try {
                        AppUser viewer = currentUserService.getCurrentUser();
                        analyticsService.trackProfileView(profile, viewer);
                    } catch (Exception ignored) {}
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}