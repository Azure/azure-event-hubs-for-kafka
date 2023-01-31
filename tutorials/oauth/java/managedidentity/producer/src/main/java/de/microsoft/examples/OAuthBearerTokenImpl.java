//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
package de.microsoft.examples;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

public class OAuthBearerTokenImpl implements OAuthBearerToken {

    private final String token;
    private final long lifetimeMs;
    private final Set<String> scopes = new HashSet<>();

    public OAuthBearerTokenImpl(final String token, final Date expiresOn) {
        this.token = token;
        this.lifetimeMs = expiresOn.getTime();
    }

    @Override
    public String value() {
        return token;
    }

    @Override
    public Set<String> scope() {
        return scopes;
    }

    @Override
    public long lifetimeMs() {
        return lifetimeMs;
    }

    @Override
    public String principalName() {
        return null;
    }

    @Override
    public Long startTimeMs() {
        return null;
    }
}
