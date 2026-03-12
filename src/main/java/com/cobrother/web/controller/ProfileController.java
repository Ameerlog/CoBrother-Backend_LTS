package com.cobrother.web.controller;

import com.cobrother.web.model.profile.ProfileUpdateRequest;
import com.cobrother.web.service.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    /**
     * GET /api/v1/profile/me
     * Returns the currently logged-in user's full profile.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile() {
        return profileService.getMyProfile();
    }

    /**
     * PUT /api/v1/profile/complete
     * Completes the user's profile by setting firstname + lastname (and optionally phone).
     * Sets profileComplete = true once done.
     *
     * Body: { "firstname": "Rahul", "lastname": "Sharma", "phoneNumber": "9876543210" }
     */
    @PutMapping("/complete")
    public ResponseEntity<?> completeProfile(@RequestBody ProfileUpdateRequest request) {
        return profileService.completeProfile(request);
    }
}