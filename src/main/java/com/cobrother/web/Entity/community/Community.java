package com.cobrother.web.Entity.community;

import com.cobrother.web.Entity.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "community")
public class Community {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Auto-filled from LinkedIn ────────────────────────────────────────────
    private String name;
    private String imageUrl;

    // LinkedIn profile URL (e.g. https://www.linkedin.com/in/username)
    private String linkedInProfileUrl;

    // Raw LinkedIn sub/id — to prevent duplicate community profiles
    @Column(unique = true)
    private String linkedInId;

    // ── Manually filled by user ──────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    private CommunityRole role;

    // Stored as comma-separated string for simplicity (e.g. "Java,Spring,React")
    private String skills;

    @Enumerated(EnumType.STRING)
    private CommunityIndustry industry;

    private String location;

    // ── Linked app user (nullable — community profile can exist independently) ─
    @OneToOne
    @JoinColumn(name = "app_user_id", nullable = false)
    private AppUser appUser;

    // ── Timestamps ───────────────────────────────────────────────────────────
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime lastModified;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.lastModified = LocalDateTime.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getLinkedInProfileUrl() { return linkedInProfileUrl; }
    public void setLinkedInProfileUrl(String linkedInProfileUrl) { this.linkedInProfileUrl = linkedInProfileUrl; }

    public String getLinkedInId() { return linkedInId; }
    public void setLinkedInId(String linkedInId) { this.linkedInId = linkedInId; }

    public CommunityRole getRole() { return role; }
    public void setRole(CommunityRole role) { this.role = role; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public CommunityIndustry getIndustry() { return industry; }
    public void setIndustry(CommunityIndustry industry) { this.industry = industry; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public AppUser getAppUser() { return appUser; }
    public void setAppUser(AppUser appUser) { this.appUser = appUser; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModified() { return lastModified; }
}