package com.cobrother.web.model.auction;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionUpdateMessage {
    private Long auctionId;
    private String type; // BID_PLACED, AUCTION_ENDED, AUCTION_EXTENDED, AUCTION_STARTED
    private double currentHighestBid;
    private String currentWinnerName;
    private int totalBids;
    private LocalDateTime endTime;
    private String status;
    private BidSummary latestBid;
    private String message;

    public static class BidSummary {
        public String bidderName;
        public double amount;
        public LocalDateTime bidTime;
        public BidSummary(String name, double amount, LocalDateTime time) {
            this.bidderName = name; this.amount = amount; this.bidTime = time;
        }
    }

    public AuctionUpdateMessage() {}

    // Getters & Setters
    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double v) { this.currentHighestBid = v; }
    public String getCurrentWinnerName() { return currentWinnerName; }
    public void setCurrentWinnerName(String v) { this.currentWinnerName = v; }
    public int getTotalBids() { return totalBids; }
    public void setTotalBids(int totalBids) { this.totalBids = totalBids; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BidSummary getLatestBid() { return latestBid; }
    public void setLatestBid(BidSummary latestBid) { this.latestBid = latestBid; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}