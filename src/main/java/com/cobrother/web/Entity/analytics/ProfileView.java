package com.cobrother.web.Entity.analytics;

import com.cobrother.web.Entity.community.Community;
import com.cobrother.web.Entity.user.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "profile_views")
public class ProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Community profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id")
    private AppUser viewer;

    private String viewerIndustry;
    private String viewerRole;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() { this.viewedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Community getProfile() { return profile; }
    public void setProfile(Community v) { this.profile = v; }
    public AppUser getViewer() { return viewer; }
    public void setViewer(AppUser v) { this.viewer = v; }
    public String getViewerIndustry() { return viewerIndustry; }
    public void setViewerIndustry(String v) { this.viewerIndustry = v; }
    public String getViewerRole() { return viewerRole; }
    public void setViewerRole(String v) { this.viewerRole = v; }
    public LocalDateTime getViewedAt() { return viewedAt; }
}