package com.cobrother.web.auth;

import com.cobrother.web.service.auth.GoogleOAuth2UserInfo;
import com.cobrother.web.service.auth.OAuth2UserInfo;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                   Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        }
        throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
    }
}