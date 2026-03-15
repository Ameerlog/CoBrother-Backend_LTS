package com.cobrother.web.Entity.user;

import com.cobrother.web.Entity.cobranding.Domain;
import com.cobrother.web.Entity.cocreation.Software;
import com.cobrother.web.Entity.community.Community;
import com.cobrother.web.Entity.coventure.CoVenture;
import com.cobrother.web.Entity.coventure.Venture;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_email", columnList = "email"),
                @Index(name = "idx_user_verification_token", columnList = "verificationToken")
        }
)
@AllArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    private String firstname;
    private String lastname;

    @JsonIgnore
    private String password;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider = AuthProvider.OAUTH;

    private String oauthProvider;
    private String oauthProviderId;

    @JsonIgnore
    @OneToOne(mappedBy = "appUser")
    private Community communityProfile;

    @Column(nullable = false)
    private Boolean profileComplete = false;

    private String phoneNumber;

    @Column(nullable = false)
    private Boolean phoneVerified = false;

    @JsonIgnore
    private String emailOtp;

    @JsonIgnore
    private LocalDateTime emailOtpExpiry;

    @JsonIgnore
    private String verificationToken;

    @JsonIgnore
    private LocalDateTime verificationTokenExpiry;

    @JsonIgnore
    private String otp;

    @JsonIgnore
    private LocalDateTime otpExpiry;

    @JsonIgnore
    private String refreshToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastModified;

    // ── All back-reference collections MUST be @JsonIgnore ──────────────────
    @JsonIgnore
    @OneToMany(mappedBy = "listedBy")
    private List<Domain> listedDomains;

    @JsonIgnore
    @OneToMany(mappedBy = "purchasedBy")
    private List<Domain> purchasedDomains;

    @JsonIgnore
    @OneToMany(mappedBy = "listedBy")
    private List<Venture> listedVentures;

    @JsonIgnore
    @OneToMany(mappedBy = "purchasedBy")
    private List<Venture> purchasedVentures;

    @JsonIgnore
    @OneToMany(mappedBy = "applicant")
    private List<CoVenture> coVenturedVentures;


    @JsonIgnore
    @OneToMany(mappedBy = "listedBy")
    private List<Software> listedSoftware;

    @JsonIgnore
    @OneToMany(mappedBy = "purchasedBy")
    private List<Software> purchasedSoftware;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.lastModified = LocalDateTime.now();
    }

    public AppUser() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    public String getOauthProvider() { return oauthProvider; }
    public void setOauthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }

    public String getOauthProviderId() { return oauthProviderId; }
    public void setOauthProviderId(String oauthProviderId) { this.oauthProviderId = oauthProviderId; }

    public Community getCommunityProfile() { return communityProfile; }
    public void setCommunityProfile(Community communityProfile) { this.communityProfile = communityProfile; }

    public Boolean getProfileComplete() { return profileComplete; }
    public void setProfileComplete(Boolean profileComplete) { this.profileComplete = profileComplete; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Boolean getPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(Boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public String getEmailOtp() { return emailOtp; }
    public void setEmailOtp(String emailOtp) { this.emailOtp = emailOtp; }

    public LocalDateTime getEmailOtpExpiry() { return emailOtpExpiry; }
    public void setEmailOtpExpiry(LocalDateTime emailOtpExpiry) { this.emailOtpExpiry = emailOtpExpiry; }

    public String getVerificationToken() { return verificationToken; }
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    public LocalDateTime getVerificationTokenExpiry() { return verificationTokenExpiry; }
    public void setVerificationTokenExpiry(LocalDateTime verificationTokenExpiry) { this.verificationTokenExpiry = verificationTokenExpiry; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public LocalDateTime getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime otpExpiry) { this.otpExpiry = otpExpiry; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public List<Domain> getListedDomains() { return listedDomains; }
    public void setListedDomains(List<Domain> listedDomains) { this.listedDomains = listedDomains; }

    public List<Domain> getPurchasedDomains() { return purchasedDomains; }
    public void setPurchasedDomains(List<Domain> purchasedDomains) { this.purchasedDomains = purchasedDomains; }

    public List<Venture> getListedVentures() { return listedVentures; }
    public void setListedVentures(List<Venture> listedVentures) { this.listedVentures = listedVentures; }

    public List<Venture> getPurchasedVentures() { return purchasedVentures; }
    public void setPurchasedVentures(List<Venture> purchasedVentures) { this.purchasedVentures = purchasedVentures; }

    public List<CoVenture> getCoVenturedVentures() { return coVenturedVentures; }
    public void setCoVenturedVentures(List<CoVenture> coVenturedVentures) { this.coVenturedVentures = coVenturedVentures; }

    public List<Software> getListedSoftware() {
        return listedSoftware;
    }

    public void setListedSoftware(List<Software> listedSoftware) {
        this.listedSoftware = listedSoftware;
    }

    public List<Software> getPurchasedSoftware() {
        return purchasedSoftware;
    }

    public void setPurchasedSoftware(List<Software> purchasedSoftware) {
        this.purchasedSoftware = purchasedSoftware;
    }
}