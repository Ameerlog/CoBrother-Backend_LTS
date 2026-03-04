package com.cobrother.web.service.auth;

import com.cobrother.web.Entity.user.AppUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserDetailsServiceImpl userDetailsService;

    public CurrentUserService(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public AppUser getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userDetailsService.getUserByEmail(email);
    }
}