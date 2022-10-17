//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
import java.util.Date;
import java.util.Set;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

public class OAuthBearerTokenImp implements OAuthBearerToken
{
    String token;
    long lifetimeMs;

    public OAuthBearerTokenImp(final String token, Date expiresOn) {
        this.token = token;
        this.lifetimeMs = expiresOn.getTime();
    }

    @Override
    public String value() {
        return this.token;
    }

    @Override
    public Set<String> scope() {
        return null;
    }

    @Override
    public long lifetimeMs() {
        return this.lifetimeMs;
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