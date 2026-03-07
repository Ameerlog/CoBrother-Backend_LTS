package com.cobrother.web.Entity.community;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── From LinkedIn (auto-filled on connect) ────────────────────────────────
    private String linkedInId;
    private String name;
    private String imageUrl;
    private String linkedInProfileUrl;

    // ── Filled by user in the form after LinkedIn connect ─────────────────────
    @Enumerated(EnumType.STRING)
    private CommunityRole role;

    private String skills;          // comma-separated: "React, Java, Finance"

    @Enumerated(EnumType.STRING)
    private CommunityIndustry industry;

    // add this field
    private LocalDateTime createdAt;

    private String location;        // e.g. "Bengaluru, India"

    // ── Relationship ───────────────────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", unique = true)
    @JsonIgnoreProperties({
            "communityProfile", "listedDomains", "purchasedDomains",
            "listedVentures", "purchasedVentures", "coVenturedVentures",
            "password", "otp", "otpExpiry", "emailOtp", "emailOtpExpiry",
            "verificationToken", "verificationTokenExpiry", "refreshToken",
            "hibernateLazyInitializer", "handler"
    })
    private AppUser appUser;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    // ── Getters & Setters ─────────────────────────────────────────────────────
    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getLinkedInId()              { return linkedInId; }
    public void setLinkedInId(String v)        { this.linkedInId = v; }

    public String getName()                    { return name; }
    public void setName(String v)              { this.name = v; }

    public String getImageUrl()                { return imageUrl; }
    public void setImageUrl(String v)          { this.imageUrl = v; }

    public String getLinkedInProfileUrl()      { return linkedInProfileUrl; }
    public void setLinkedInProfileUrl(String v){ this.linkedInProfileUrl = v; }

    public CommunityRole getRole()             { return role; }
    public void setRole(CommunityRole v)       { this.role = v; }

    public String getSkills()                  { return skills; }
    public void setSkills(String v)            { this.skills = v; }

    public CommunityIndustry getIndustry()     { return industry; }
    public void setIndustry(CommunityIndustry v){ this.industry = v; }

    public String getLocation()                { return location; }
    public void setLocation(String v)          { this.location = v; }

    public AppUser getAppUser()                { return appUser; }
    public void setAppUser(AppUser v)          { this.appUser = v; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}