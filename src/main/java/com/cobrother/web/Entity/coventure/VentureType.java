package com.cobrother.web.Entity.coventure;


public enum VentureType {

    FIFTY_FIFTY("50:50", "Equal Synergy"),
    SIXTY_FORTY("60:40", "Majority Founder"),
    SEVENTY_THIRTY("70:30", "Strategic Growth"),
    EIGHTY_TWENTY("80:20", "Advisor / Investor Stake"),
    NINETY_TEN("90:10", "Minor Equity Placement"),
    NEGOTIABLE("Negotiable", "Request Custom Structure");

    private final String ratio;
    private final String description;

    VentureType(String ratio, String description) {
        this.ratio = ratio;
        this.description = description;
    }

    public String getRatio() {
        return ratio;
    }

    public String getDescription() {
        return description;
    }
}
