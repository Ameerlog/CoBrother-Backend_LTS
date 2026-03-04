package com.cobrother.web.Entity.coventure;

import jakarta.persistence.Embeddable;

@Embeddable
public class Agreement {

    private boolean terms;

    public boolean isTermsAccepted() {
        return terms;
    }

    public void setTermsAccepted(boolean terms) {
        this.terms = terms;
    }

}
