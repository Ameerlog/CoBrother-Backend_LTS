package com.cobrother.web.Entity.cobranding;
public enum AuctionDuration {
    ONE_DAY(1), SEVEN_DAYS(7), FIFTEEN_DAYS(15), THIRTY_DAYS(30);

    private final int days;
    AuctionDuration(int days) { this.days = days; }
    public int getDays() { return days; }
}