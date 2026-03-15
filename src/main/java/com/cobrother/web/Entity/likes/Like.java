package com.cobrother.web.Entity.likes;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "likes",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_like_user_entity",
                columnNames = {"user_id", "entity_id", "like_type"}
        )
)
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken",
            "hibernateLazyInitializer","listedSoftware","purchasedSoftware"})
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LikeType likeType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    public Like() {}

    public Like(AppUser user, LikeType likeType, Long entityId) {
        this.user = user;
        this.likeType = likeType;
        this.entityId = entityId;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public LikeType getLikeType() { return likeType; }
    public void setLikeType(LikeType likeType) { this.likeType = likeType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}