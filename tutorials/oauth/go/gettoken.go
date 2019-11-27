//Copyright (c) Microsoft Corporation. All rights reserved.
//Copyright 2016 Confluent Inc.
//Licensed under the MIT License.
//Licensed under the Apache License, Version 2.0
//
//Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

package main

import (
	"fmt"
	//"github.com/Azure/go-autorest/autorest"
	"github.com/Azure/go-autorest/autorest/adal"
	//"github.com/Azure/go-autorest/autorest/azure"
	//"github.com/Azure/go-autorest/autorest/date"
)

func main() {
	fmt.Println("hello world");

	applicationID := "fd756bd0-6853-41fd-8f5a-cc7e8bd89214"
	applicationSecret := "l.vUN-4SRE45Ph9@D-]_s[zUaOBchc-i"
	tenantID := "72f988bf-86f1-41af-91ab-2d7cd011db47"
	resource := "https://eventhubs.azure.net/"

	const activeDirectoryEndpoint = "https://login.microsoftonline.com/"
	oauthConfig, err := adal.NewOAuthConfig(activeDirectoryEndpoint, tenantID)

	callback := func(token adal.Token) error {
		// This is called after the token is acquired
		return nil;
	}

	spt, err := adal.NewServicePrincipalToken(
		*oauthConfig,
		applicationID,
		applicationSecret,
		resource,
		callback)

	if err != nil {
		fmt.Println(err);
	}
	
	// Acquire a new access token
	err  = spt.Refresh()
	if (err == nil) {
		token := spt.OAuthToken()
		fmt.Println("Token " + token);
	}
}
