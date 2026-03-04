package com.cobrother.web.model;

import com.cobrother.web.Entity.user.UserRole;

public class JwtResponseDto {

    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private UserRole role;
    private Long expiresIn; // in milliseconds
    private Boolean isNewUser; // To indicate if this is a first-time login
    private Boolean emailVerified;

    public JwtResponseDto() {}

    public JwtResponseDto(String accessToken, String refreshToken, Long userId, String email,
                          UserRole role, Long expiresIn, Boolean isNewUser, Boolean emailVerified) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.expiresIn = expiresIn;
        this.isNewUser = isNewUser;
        this.emailVerified = emailVerified;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Boolean getNewUser() {
        return isNewUser;
    }

    public void setNewUser(Boolean newUser) {
        isNewUser = newUser;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}