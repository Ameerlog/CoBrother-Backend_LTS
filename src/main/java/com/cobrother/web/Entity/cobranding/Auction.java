package com.cobrother.web.Entity.cobranding;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"auction","hibernateLazyInitializer"})
    private Domain domain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionDuration duration;

    private double minBidPrice;
    private double currentHighestBid = 0;
    private int totalBids = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_winner_id")
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken",
            "hibernateLazyInitializer","listedSoftware","purchasedSoftware"})
    private AppUser currentWinner;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime; // before any anti-snipe extensions

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<AuctionBid> bids = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // Getters & Setters
    public Long getId() { return id; }
    public Domain getDomain() { return domain; }
    public void setDomain(Domain domain) { this.domain = domain; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public AuctionDuration getDuration() { return duration; }
    public void setDuration(AuctionDuration duration) { this.duration = duration; }
    public double getMinBidPrice() { return minBidPrice; }
    public void setMinBidPrice(double minBidPrice) { this.minBidPrice = minBidPrice; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
    public int getTotalBids() { return totalBids; }
    public void setTotalBids(int totalBids) { this.totalBids = totalBids; }
    public AppUser getCurrentWinner() { return currentWinner; }
    public void setCurrentWinner(AppUser currentWinner) { this.currentWinner = currentWinner; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getOriginalEndTime() { return originalEndTime; }
    public void setOriginalEndTime(LocalDateTime t) { this.originalEndTime = t; }
    public List<AuctionBid> getBids() { return bids; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setBids(List<AuctionBid> bids) {
        this.bids = bids;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}