package com.cobrother.web.service.auth;

import java.util.Map;

/**
 * Abstracts the attributes map returned by each OAuth provider
 * so the rest of the app doesn't care which provider was used.
 */
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** Provider-unique user ID */
    public abstract String getId();

    /** Email address */
    public abstract String getEmail();

    /** First name (may be null if provider doesn't split names) */
    public abstract String getFirstName();

    /** Last name (may be null) */
    public abstract String getLastName();
}