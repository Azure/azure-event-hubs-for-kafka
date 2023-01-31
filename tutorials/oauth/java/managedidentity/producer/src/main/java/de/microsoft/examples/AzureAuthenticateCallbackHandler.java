//Copyright (c) Microsoft Corporation. All rights reserved.
//Licensed under the MIT License.
package de.microsoft.examples;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;

public class AzureAuthenticateCallbackHandler implements AuthenticateCallbackHandler {

    private String requestedScope;

    @Override
    public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
        for (Callback callback: callbacks) {
            if (!(callback instanceof OAuthBearerTokenCallback)) {
                throw new UnsupportedCallbackException(callback);
            }

            try {
                OAuthBearerToken token = getOAuthBearerToken();
                OAuthBearerTokenCallback oauthCallback = (OAuthBearerTokenCallback) callback;
                oauthCallback.token(token);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | TimeoutException | ParseException e) {
                throw new RuntimeException("Failed to get token from azure.", e);
            }
        }
    }

    private OAuthBearerToken getOAuthBearerToken() throws InterruptedException, ExecutionException,
        TimeoutException, ParseException {
        final TokenCredential defaultCredential = new DefaultAzureCredentialBuilder()
            .build();

        final TokenRequestContext tokenRequestContext = new TokenRequestContext()
            .addScopes(requestedScope);

        final AccessToken accessToken = defaultCredential
            .getTokenSync(tokenRequestContext);
        return mapToOAuthBearerToken(accessToken);
    }

    private static OAuthBearerTokenImpl mapToOAuthBearerToken(AccessToken value) {
        try{
            final JWT jwt = JWTParser.parse(value.getToken());
            return new OAuthBearerTokenImpl(value.getToken(), jwt.getJWTClaimsSet().getExpirationTime());
        } catch(ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws KafkaException {
        // NOOP
    }

    @Override
    public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
        String bootstrapServer = extractFirstBootstrapServer(configs);

        final String hostname = bootstrapServer.split(":")[0];
        this.requestedScope = "https://" + hostname + "/.default";
    }

    private static String extractFirstBootstrapServer(Map<String, ?> configs) {
        if (!configs.containsKey(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
            throw new IllegalStateException("Missing bootstrap.servers in kafka configuration.");
        }

        if (!(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG) instanceof List<?> bootstrapServersList)) {
            throw new IllegalStateException(
                "bootstrap.servers in kafka configuration is not a String value");
        }

        if (bootstrapServersList.size() > 1) {
            throw new IllegalStateException("More than 1 bootstrap.servers not supported in this example.");
        }

        if (!(bootstrapServersList.get(0) instanceof String bootstrapServer)) {
            throw new IllegalStateException("bootstrap.servers has to be a String.");
        }
        return bootstrapServer;
    }

}
