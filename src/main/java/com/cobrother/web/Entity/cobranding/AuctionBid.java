package com.cobrother.web.Entity.cobranding;

import com.cobrother.web.Entity.user.AppUser;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_bids", indexes = {
        @Index(name = "idx_bid_auction", columnList = "auction_id"),
        @Index(name = "idx_bid_time",    columnList = "bidTime")
})
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    @JsonIgnoreProperties("bids")
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    @JsonIgnoreProperties({"listedDomains","purchasedDomains","listedVentures",
            "purchasedVentures","coVenturedVentures","communityProfile",
            "password","otp","otpExpiry","emailOtp","emailOtpExpiry",
            "verificationToken","verificationTokenExpiry","refreshToken",
            "hibernateLazyInitializer","listedSoftware","purchasedSoftware"})
    private AppUser bidder;

    private double amount;
    private String bidderName; // snapshot at bid time

    @Column(nullable = false)
    private LocalDateTime bidTime;

    private boolean isWinningBid = false;

    @PrePersist
    protected void onCreate() { this.bidTime = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Auction getAuction() { return auction; }
    public void setAuction(Auction auction) { this.auction = auction; }
    public AppUser getBidder() { return bidder; }
    public void setBidder(AppUser bidder) { this.bidder = bidder; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    public LocalDateTime getBidTime() { return bidTime; }
    public boolean isWinningBid() { return isWinningBid; }
    public void setWinningBid(boolean winningBid) { isWinningBid = winningBid; }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    public void setId(Long id) {
        this.id = id;
    }
}