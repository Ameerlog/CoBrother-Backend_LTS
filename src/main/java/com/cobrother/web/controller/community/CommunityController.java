package com.cobrother.web.controller.community;

import com.cobrother.web.dto.community.CommunityUpdateRequest;
import com.cobrother.web.service.community.CommunityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    /**
     * GET /api/v1/community/linkedin/auth
     * Returns the LinkedIn OAuth URL — frontend redirects user to this.
     */
    @GetMapping("/linkedin/auth")
    public ResponseEntity<String> getLinkedInAuthUrl() {
        return communityService.getLinkedInAuthUrl();
    }

    /**
     * GET /api/v1/community/linkedin/callback?code=...
     * LinkedIn redirects here after user approves.
     * Exchanges code, fetches name + photo, saves Community profile.
     */
    @GetMapping("/linkedin/callback")
    public ResponseEntity<?> linkedInCallback(@RequestParam String code) {
        return communityService.handleLinkedInCallback(code);
    }

    /**
     * PUT /api/v1/community/{id}
     * User manually fills in role, skills, industry, location.
     * Body: { "role": "FOUNDER", "skills": "Java,Spring", "industry": "TECH", "location": "Bengaluru" }
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Long id,
            @RequestBody CommunityUpdateRequest request) {
        return communityService.updateCommunityProfile(id, request);
    }

    /**
     * GET /api/v1/community/all
     * Fetch all community profiles for listing page.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllProfiles() {
        return communityService.getAllProfiles();
    }

    /**
     * GET /api/v1/community/{id}
     * Fetch a single community profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return communityService.getProfile(id);
    }
}