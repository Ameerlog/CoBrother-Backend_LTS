package com.cobrother.web.service.auth;

import com.cobrother.web.Entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final String username; // Using email as username
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean active;
    private final boolean emailVerified;
    private final Long userId;

    public CustomUserDetails(AppUser appUser) {
        this.username = appUser.getEmail();
        this.password = appUser.getPassword();
        this.active = appUser.getActive();
        this.emailVerified = appUser.getEmailVerified();
        this.userId = appUser.getId();

        // Create authorities from user role
        List<GrantedAuthority> auths = new ArrayList<>();
        if (appUser.getRole() != null) {
            auths.add(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()));
        } else {
            auths.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        this.authorities = auths;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active && emailVerified;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }
}