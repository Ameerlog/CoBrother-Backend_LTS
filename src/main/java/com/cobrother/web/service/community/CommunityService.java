package com.cobrother.web.service.community;

import com.cobrother.web.Entity.community.Community;
import com.cobrother.web.Repository.CommunityRepository;
import com.cobrother.web.model.community.CommunityUpdateRequest;
import com.cobrother.web.service.auth.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Service
public class CommunityService {

    @Value("${linkedin.client-id}")
    private String clientId;

    @Value("${linkedin.client-secret}")
    private String clientSecret;

    @Value("${linkedin.redirect-uri}")
    private String redirectUri;

    @Autowired
    private CommunityRepository communityRepository;

    @Autowired
    private CurrentUserService currentUserService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Step 1: Generate LinkedIn OAuth URL ─────────────────────────────────
    public ResponseEntity<String> getLinkedInAuthUrl() {
        String url = "https://www.linkedin.com/oauth/v2/authorization"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=openid%20profile%20email";

        return ResponseEntity.ok(url);
    }

    // ── Step 2: Handle callback, exchange code, fetch profile, save ──────────
    public ResponseEntity<?> handleLinkedInCallback(String code) {
        try {
            // Exchange authorization code for access token
            String accessToken = exchangeCodeForToken(code);
            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("Failed to obtain access token from LinkedIn.");
            }

            // Fetch user profile using OpenID userinfo endpoint
            JsonNode profile = fetchLinkedInProfile(accessToken);
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body("Failed to fetch profile from LinkedIn.");
            }

            String linkedInId   = profile.path("sub").asText();
            String name         = profile.path("name").asText();        // full name
            String imageUrl     = profile.path("picture").asText();     // profile pic URL
            String profileUrl   = "https://www.linkedin.com/in/" + profile.path("given_name").asText().toLowerCase();

            // Upsert: if this LinkedIn account already has a community profile, return it
            Optional<Community> existing = communityRepository.findByLinkedInId(linkedInId);
            if (existing.isPresent()) {
                return ResponseEntity.ok(existing.get());
            }

            // Create new community profile
            Community community = new Community();
            community.setLinkedInId(linkedInId);
            community.setName(name);
            community.setImageUrl(imageUrl);
            community.setLinkedInProfileUrl(profileUrl);

            // Link to logged-in AppUser if present (optional — won't fail if not authenticated)
            try {
                community.setAppUser(currentUserService.getCurrentUser());
            } catch (Exception ignored) {}

            return ResponseEntity.ok(communityRepository.save(community));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Something went wrong during LinkedIn authentication.");
        }
    }

    // ── Step 3: User manually fills in role, skills, industry, location ──────
    public ResponseEntity<?> updateCommunityProfile(Long id, CommunityUpdateRequest request) {
        try {
            Community community = communityRepository.findById(id)
                    .orElse(null);

            if (community == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Community profile not found.");
            }

            if (request.getRole()     != null) community.setRole(request.getRole());
            if (request.getSkills()   != null) community.setSkills(request.getSkills());
            if (request.getIndustry() != null) community.setIndustry(request.getIndustry());
            if (request.getLocation() != null) community.setLocation(request.getLocation());

            return ResponseEntity.ok(communityRepository.save(community));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update community profile.");
        }
    }

    // ── Get all community profiles (for community listing page) ──────────────
    public ResponseEntity<?> getAllProfiles() {
        try {
            return ResponseEntity.ok(communityRepository.findAll());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to fetch community profiles.");
        }
    }

    // ── Get single profile ────────────────────────────────────────────────────
    public ResponseEntity<?> getProfile(Long id) {
        return communityRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found."));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String exchangeCodeForToken(String code) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type",    "authorization_code");
            body.add("code",          code);
            body.add("redirect_uri",  redirectUri);
            body.add("client_id",     clientId);
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://www.linkedin.com/oauth/v2/accessToken", request, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            return json.path("access_token").asText(null);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JsonNode fetchLinkedInProfile(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // OpenID Connect userinfo endpoint — returns sub, name, picture, email
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.linkedin.com/v2/userinfo",
                    HttpMethod.GET, request, String.class);

            return objectMapper.readTree(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}