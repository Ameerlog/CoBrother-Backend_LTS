package com.cobrother.web.model.auth;

public class OtpRequestDto {

    private String email;

    public OtpRequestDto() {}

    public OtpRequestDto(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}