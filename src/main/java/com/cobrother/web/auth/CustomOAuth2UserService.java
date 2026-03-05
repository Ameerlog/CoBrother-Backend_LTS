package com.cobrother.web.auth;

import com.cobrother.web.Entity.user.AppUser;
import com.cobrother.web.Entity.user.AuthProvider;
import com.cobrother.web.Entity.user.UserRole;
import com.cobrother.web.Repository.UserRepository;
import com.cobrother.web.service.auth.CustomUserDetails;
import com.cobrother.web.service.auth.OAuth2UserInfo;
import com.cobrother.web.service.auth.oauth2.OAuth2UserInfoFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CustomOAuth2UserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Let Spring load the OIDC user first (validates ID token, fetches userinfo)
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oidcUser.getAttributes());

        String email = userInfo.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not returned by OAuth2 provider");
        }

        Optional<AppUser> existingUser = userRepository.findByEmail(email.toLowerCase().trim());

        AppUser user;
        if (existingUser.isPresent()) {
            user = updateExistingOAuthUser(existingUser.get(), userInfo, registrationId);
        } else {
            user = createNewOAuthUser(userInfo, registrationId);
        }

        // Wrap in CustomUserDetails, passing OIDC claims + ID token so Spring is happy
        return new CustomUserDetails(user, oidcUser.getAttributes(), oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private AppUser createNewOAuthUser(OAuth2UserInfo userInfo, String registrationId) {
        AppUser user = new AppUser();
        user.setEmail(userInfo.getEmail().toLowerCase().trim());
        user.setEmailVerified(true);
        user.setActive(true);
        user.setRole(UserRole.GUEST);
        user.setAuthProvider(AuthProvider.OAUTH);
        user.setOauthProvider(registrationId);
        user.setOauthProviderId(userInfo.getId());
        user.setProfileComplete(false);
        return userRepository.save(user);
    }

    private AppUser updateExistingOAuthUser(AppUser user, OAuth2UserInfo userInfo,
                                            String registrationId) {
        user.setOauthProvider(registrationId);
        user.setOauthProviderId(userInfo.getId());
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}