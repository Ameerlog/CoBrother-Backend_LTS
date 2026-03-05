package com.cobrother.web.service.auth;

import com.cobrother.web.Entity.user.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Unified principal for JWT filter (UserDetails) and Google OIDC login (OidcUser).
 */
public class CustomUserDetails implements UserDetails, OidcUser {

    private final Long userId;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;
    private final boolean emailVerified;
    private final boolean profileComplete;
    private final boolean isNewUser;

    // OIDC-specific — null when loaded via JWT path
    private final Map<String, Object> attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    /** JWT filter path — no OIDC data needed */
    public CustomUserDetails(AppUser appUser) {
        this(appUser, null, null, null);
    }

    /** OIDC login path */
    public CustomUserDetails(AppUser appUser, Map<String, Object> attributes,
                             OidcIdToken idToken, OidcUserInfo userInfo) {
        this.userId = appUser.getId();
        this.username = appUser.getEmail();
        this.password = appUser.getPassword();
        this.active = Boolean.TRUE.equals(appUser.getActive());
        this.emailVerified = Boolean.TRUE.equals(appUser.getEmailVerified());
        this.profileComplete = Boolean.TRUE.equals(appUser.getProfileComplete());
        this.isNewUser = !this.profileComplete;
        this.attributes = attributes != null ? attributes : Map.of();
        this.idToken = idToken;
        this.userInfo = userInfo;

        List<GrantedAuthority> auths = new ArrayList<>();
        if (appUser.getRole() != null) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));
        } else {
            auths.add(new SimpleGrantedAuthority("ROLE_GUEST"));
        }
        this.authorities = auths;
    }

    // ── UserDetails ──────────────────────────────────────────────────────────

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active && emailVerified; }

    // ── OidcUser / OAuth2User ────────────────────────────────────────────────

    @Override public Map<String, Object> getClaims() { return attributes; }
    @Override public OidcUserInfo getUserInfo() { return userInfo; }
    @Override public OidcIdToken getIdToken() { return idToken; }
    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() { return username; }

    // ── Custom ───────────────────────────────────────────────────────────────

    public Long getUserId() { return userId; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isProfileComplete() { return profileComplete; }
    public boolean isNewUser() { return isNewUser; }
}