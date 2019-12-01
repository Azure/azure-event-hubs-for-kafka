//Copyright (c) Microsoft Corporation. All rights reserved.
//Copyright 2016 Confluent Inc.
//Licensed under the MIT License.
//Licensed under the Apache License, Version 2.0
//
//Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

package main

import (
	"fmt"
	"os"

	"github.com/Azure/go-autorest/autorest/adal"
)

func main() {
	fmt.Println("hello world")

	tenantID := os.Getenv("AAD_TENANT_ID")
	applicationID := os.Getenv("AAD_APPLICATION_ID")
	applicationSecret := os.Getenv("AAD_APPLICATION_SECRET")
	resource := "https://serkant-test1.servicebus.windows.net/"

	const activeDirectoryEndpoint = "https://login.microsoftonline.com/"
	oauthConfig, err := adal.NewOAuthConfig(activeDirectoryEndpoint, tenantID)

	callback := func(token adal.Token) error {
		// This is called after the token is acquired
		return nil
	}

	spt, err := adal.NewServicePrincipalToken(
		*oauthConfig,
		applicationID,
		applicationSecret,
		resource,
		callback)

	if err != nil {
		fmt.Println(err)
	}

	// Acquire a new access token
	err = spt.Refresh()
	if err == nil {
		token := spt.OAuthToken()
		fmt.Println("Token " + token)
	}
}
