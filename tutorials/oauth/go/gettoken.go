//Copyright (c) Microsoft Corporation. All rights reserved.
//Copyright 2016 Confluent Inc.
//Licensed under the MIT License.
//Licensed under the Apache License, Version 2.0
//
//Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

package main

import (
	"fmt"
	"github.com/Azure/go-autorest/autorest"
	"github.com/Azure/go-autorest/autorest/adal"
	"github.com/Azure/go-autorest/autorest/azure"
	"github.com/Azure/go-autorest/autorest/date"
)

func main() {
	fmt.Println("hello world");

	applicationID := "fd756bd0-6853-41fd-8f5a-cc7e8bd89214"
	applicationSecret := "l.vUN-4SRE45Ph9@D-]_s[zUaOBchc-i"
	tenantID := "72f988bf-86f1-41af-91ab-2d7cd011db47"

	const activeDirectoryEndpoint = "https://login.microsoftonline.com/"
	oauthConfig, err := adal.NewOAuthConfig(activeDirectoryEndpoint, tenantID)


	spt, err := adal.NewServicePrincipalToken(
		*oauthConfig,
		appliationID,
		applicationSecret,
		resource,
		callbacks...)
	if err != nil {
		return nil, err
	}
	
	// Acquire a new access token
	err  = spt.Refresh()
	if (err == nil) {
		token := spt.Token
		fmt.Println("Token " + token);
	}
}
