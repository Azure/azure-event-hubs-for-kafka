package main

import (
	"os"
	"fmt"

	"github.com/Shopify/sarama"
	"github.com/Azure/go-autorest/autorest/adal"
)

// TokenProvider is a simple struct that implements sarama.AccessTokenProvider.
// It encapsulates an oauth2.TokenSource which is leveraged for AccessToken retrieval through the
// oauth2 client credentials flow, the token will auto-refresh as necessary.
type TokenProvider struct {
	servicePrincipalToken *adal.ServicePrincipalToken
}

// NewTokenProvider creates a new sarama.AccessTokenProvider with the provided clientID and clientSecret.
// The provided tokenURL is used to perform the 2 legged client credentials flow.
func NewTokenProvider() sarama.AccessTokenProvider {
	spt, err := getServicePrincipalToken(
		os.Getenv("AAD_TENANT_ID"),
		os.Getenv("AAD_APPLICATION_ID"),
		os.Getenv("AAD_APPLICATION_SECRET"),
		os.Getenv("AAD_AUDIENCE"))
	if err != nil {
		return nil
	}

	return &TokenProvider{
		servicePrincipalToken : spt,
	}
}

// Token returns a new *sarama.AccessToken or an error as appropriate.
func (t *TokenProvider) Token() (*sarama.AccessToken, error) {
	err := t.servicePrincipalToken.Refresh()
	if err != nil {
		fmt.Println("Failed to acquire token. Error: ", err)
		return nil, err
	}

	accessToken := t.servicePrincipalToken.OAuthToken()

	return &sarama.AccessToken{Token: accessToken}, nil
}

func getServicePrincipalToken(tenantID, applicationID, applicationSecret, audience string) (*adal.ServicePrincipalToken, error) {
	oauthConfig, err := adal.NewOAuthConfig("https://login.microsoftonline.com/", tenantID)

	callback := func(token adal.Token) error {
		// This is called after the token is acquired
		return nil
	}

	spt, err := adal.NewServicePrincipalToken(
		*oauthConfig,
		applicationID,
		applicationSecret,
		audience,
		callback)

	return spt, err
}