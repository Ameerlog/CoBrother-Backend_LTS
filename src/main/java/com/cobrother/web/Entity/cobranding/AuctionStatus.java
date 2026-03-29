package com.cobrother.web.Entity.cobranding;

public enum AuctionStatus {
    DRAFT,        // listed but verification pending
    ACTIVE,       // verification done, auction live
    EXTENDED,     // anti-snipe extension active
    ENDED,        // auction ended, winner determined
    UNSOLD,       // auction ended with no bids
    CLOSED        // admin closed / lister took down
}
