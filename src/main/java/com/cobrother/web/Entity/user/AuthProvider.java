package com.cobrother.web.Entity.user;

public enum AuthProvider {
    OAUTH,        // Google (and other future OAuth providers) → role GUEST
    PHONE_OTP     // Mobile + OTP → role USER (future)
}