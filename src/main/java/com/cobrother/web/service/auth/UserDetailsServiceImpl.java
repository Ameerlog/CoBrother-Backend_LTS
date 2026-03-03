package com.cobrother.web.service.auth;

import com.cobrother.web.Entity.AppUser;
import com.cobrother.web.Repository.UserRepository;
import com.cobrother.web.model.UserInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(appUser);
    }

    /**
     * Get user by email
     */
    public AppUser getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Update user profile
     */
    @Transactional
    public AppUser updateUserProfile(String email, UserInfoDto userInfoDto) {
        AppUser user = getUserByEmail(email);
        return userRepository.save(user);
    }

    /**
     * Complete user profile (for first-time users)
     */
    @Transactional
    public void completeUserProfile(String email, UserInfoDto userInfoDto) {
        AppUser user = getUserByEmail(email);
        userRepository.save(user);
    }
}