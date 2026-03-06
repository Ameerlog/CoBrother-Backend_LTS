package com.cobrother.web.service.profile;

import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.UserRepository;
import com.cobrother.web.model.profile.ProfileUpdateRequest;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private UserRepository userRepository;

    // ── Get current user's profile ────────────────────────────────────────────
    public ResponseEntity<?> getMyProfile() {
        try {
            return ResponseEntity.ok(currentUserService.getCurrentUser());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Could not retrieve profile. Please log in.");
        }
    }

    // ── Complete profile (firstname + lastname required, phone optional) ───────
    public ResponseEntity<?> completeProfile(ProfileUpdateRequest request) {
        try {
            AppUser user = currentUserService.getCurrentUser();

            // firstname and lastname are mandatory to mark profile as complete
            if (request.getFirstname() == null || request.getFirstname().isBlank()) {
                return ResponseEntity.badRequest().body("First name is required.");
            }
            if (request.getLastname() == null || request.getLastname().isBlank()) {
                return ResponseEntity.badRequest().body("Last name is required.");
            }

            user.setFirstname(request.getFirstname().trim());
            user.setLastname(request.getLastname().trim());

            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
                user.setPhoneNumber(request.getPhoneNumber().trim());
            }

            user.setProfileComplete(true);

            return ResponseEntity.ok(userRepository.save(user));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to complete profile.");
        }
    }
}