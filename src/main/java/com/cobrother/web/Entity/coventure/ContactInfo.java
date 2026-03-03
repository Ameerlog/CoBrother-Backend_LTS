package com.cobrother.web.Entity.coventure;

import jakarta.persistence.Embeddable;

@Embeddable
public class ContactInfo {

    private String email;
    private String phoneNumber;       // 10-digit number



    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
