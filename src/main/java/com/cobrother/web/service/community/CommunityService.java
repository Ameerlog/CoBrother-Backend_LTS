package com.cobrother.web.service.community;

import com.cobrother.web.Entity.community.Community;
import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Repository.CommunityRepository;
import com.cobrother.web.model.community.CommunityUpdateRequest;
import com.cobrother.web.service.auth.CurrentUserService;
import com.cobrother.web.service.auth.JwtService;
import com.cobrother.web.service.auth.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class CommunityService {

    @Value("${linkedin.client-id}")       private String clientId;
    @Value("${linkedin.client-secret}")   private String clientSecret;
    @Value("${linkedin.redirect-uri}")    private String redirectUri;

    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private CommunityRepository communityRepository;
    @Autowired private CurrentUserService  currentUserService;

    private final RestTemplate rest = new RestTemplate();

    public ResponseEntity<String> getLinkedInAuthUrl() {
        AppUser me = currentUserService.getCurrentUser();
        String state = encode(me.getEmail()); // just pass email directly
        String enc = encode(redirectUri);
        String url = "https://www.linkedin.com/oauth/v2/authorization"
                + "?response_type=code&client_id=" + clientId
                + "&redirect_uri=" + enc
                + "&state=" + state
                + "&scope=openid%20profile%20email%20r_profile_basicinfo";
        return ResponseEntity.ok("{\"url\":\"" + url + "\"}");
    }

    public Long handleLinkedInCallbackAndReturnId(String code, String state) {
        String token = exchangeCode(code);
        Map<String, Object> info = fetchUserInfo(token);

        String sub     = String.valueOf(info.get("sub"));
        String name    = (String) info.getOrDefault("name", "");
        String picture = (String) info.getOrDefault("picture", "");

        String firstName = (String) info.getOrDefault("given_name", "");
        String lastName  = (String) info.getOrDefault("family_name", "");
        String profileUrl = "https://www.linkedin.com/search/results/people/?firstName="
                + encode(firstName) + "&lastName=" + encode(lastName);

        String email = java.net.URLDecoder.decode(state, StandardCharsets.UTF_8);
        AppUser me = userDetailsService.getUserByEmail(email);

        // ✅ Check if this LinkedIn ID is already linked to a DIFFERENT user
        Optional<Community> existingByLinkedIn = communityRepository.findByLinkedInId(sub);
        if (existingByLinkedIn.isPresent() &&
                !existingByLinkedIn.get().getAppUser().getId().equals(me.getId())) {
            throw new RuntimeException("This LinkedIn account is already connected to another user.");
        }

        Community c = communityRepository.findByAppUserId(me.getId())
                .orElseGet(Community::new);
        c.setLinkedInId(sub);
        c.setName(name);
        c.setImageUrl(picture);
        c.setLinkedInProfileUrl(profileUrl);
        c.setAppUser(me);

        return communityRepository.save(c).getId();
    }
    
    private String fetchProfileUrl(String token) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            h.set("LinkedIn-Version", "202401");

            ResponseEntity<Map> resp = rest.exchange(
                    "https://api.linkedin.com/v2/userinfo",  // same endpoint you already use
                    HttpMethod.GET,
                    new HttpEntity<>(h),
                    Map.class
            );

            System.out.println("Full userinfo response: " + resp.getBody());
            // Log every key available
            if (resp.getBody() != null) {
                resp.getBody().forEach((k, v) ->
                        System.out.println("Key: " + k + " = " + v));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }

    private String fetchVanityUrl(String token, String sub) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setBearerAuth(token);
            h.set("LinkedIn-Version", "202401");
            h.set("X-Restli-Protocol-Version", "2.0.0");

            ResponseEntity<Map> resp = rest.exchange(
                    "https://api.linkedin.com/v2/people/(id:" + sub + ")?projection=(id,vanityName)",
                    HttpMethod.GET,
                    new HttpEntity<>(h),
                    Map.class
            );

            if (resp.getBody() != null && resp.getBody().containsKey("vanityName")) {
                return "https://www.linkedin.com/in/" + resp.getBody().get("vanityName");
            }
        } catch (Exception e) {
            System.out.println("Could not fetch vanity URL: " + e.getMessage());
        }
        // fallback
        return "https://www.linkedin.com/search/results/people/?keywords="
                + java.net.URLEncoder.encode(sub, StandardCharsets.UTF_8);
    }

    public ResponseEntity<?> updateCommunityProfile(Long id, CommunityUpdateRequest req) {
        Optional<Community> opt = communityRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Community c = opt.get();
        AppUser me = currentUserService.getCurrentUser();
        if (!c.getAppUser().getId().equals(me.getId()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only edit your own profile."));
        if (req.getRole()     != null) c.setRole(req.getRole());
        if (req.getSkills()   != null) c.setSkills(req.getSkills());
        if (req.getIndustry() != null) c.setIndustry(req.getIndustry());
        if (req.getLocation() != null) c.setLocation(req.getLocation());
        if (req.getWhyImHere() !=null) c.setWhyImHere(req.getWhyImHere());
        return ResponseEntity.ok(communityRepository.save(c));
    }

    public ResponseEntity<?> getAllProfiles() {
        return ResponseEntity.ok(communityRepository.findAll());
    }

    public ResponseEntity<?> getProfile(Long id) {
        return communityRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<?> getMyProfile() {
        AppUser me = currentUserService.getCurrentUser();
        return communityRepository.findByAppUserId(me.getId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private String exchangeCode(String code) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String,String> body = new LinkedMultiValueMap<>();
        body.add("grant_type",    "authorization_code");
        body.add("code",          code);
        body.add("redirect_uri",  redirectUri);
        body.add("client_id",     clientId);
        body.add("client_secret", clientSecret);
        Map<?,?> resp = rest.postForObject(
                "https://www.linkedin.com/oauth/v2/accessToken",
                new HttpEntity<>(body, h), Map.class);
        if (resp == null || !resp.containsKey("access_token"))
            throw new RuntimeException("LinkedIn token exchange failed");
        return (String) resp.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchUserInfo(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        ResponseEntity<Map> resp = rest.exchange(
                "https://api.linkedin.com/v2/userinfo",
                HttpMethod.GET, new HttpEntity<>(h), Map.class);
        if (resp.getBody() == null) throw new RuntimeException("LinkedIn userinfo empty");
        return (Map<String, Object>) resp.getBody();
    }

    private String encode(String s) {
        try { return java.net.URLEncoder.encode(s, "UTF-8"); }
        catch (Exception e) { return s; }
    }

    public Optional<Community> getCommunityEntity(Long id) {
        return communityRepository.findById(id);
    }
}
