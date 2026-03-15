package com.cobrother.web.Entity.cocreation;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "software_views")
public class SoftwareView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "software_id", nullable = false)
    @JsonIgnoreProperties("softwareViews")
    private Software software;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken","hibernateLazyInitializer"})
    private AppUser viewer;

    private String viewerIndustry;
    private String viewerRole;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() { this.viewedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Software getSoftware() { return software; }
    public void setSoftware(Software software) { this.software = software; }
    public AppUser getViewer() { return viewer; }
    public void setViewer(AppUser viewer) { this.viewer = viewer; }
    public String getViewerIndustry() { return viewerIndustry; }
    public void setViewerIndustry(String viewerIndustry) { this.viewerIndustry = viewerIndustry; }
    public String getViewerRole() { return viewerRole; }
    public void setViewerRole(String viewerRole) { this.viewerRole = viewerRole; }
    public LocalDateTime getViewedAt() { return viewedAt; }
}