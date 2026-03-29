package com.cobrother.web.model.auction;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidQueueItem implements Serializable {
    private Long auctionId;
    private Long bidderId;
    private String bidderName;
    private double amount;
    private LocalDateTime queuedAt;

    public BidQueueItem() {}

    public BidQueueItem(Long auctionId, Long bidderId, String bidderName,
                        double amount) {
        this.auctionId  = auctionId;
        this.bidderId   = bidderId;
        this.bidderName = bidderName;
        this.amount     = amount;
        this.queuedAt   = LocalDateTime.now();
    }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }
    public Long getBidderId() { return bidderId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }
    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDateTime getQueuedAt() { return queuedAt; }
    public void setQueuedAt(LocalDateTime queuedAt) { this.queuedAt = queuedAt; }
}