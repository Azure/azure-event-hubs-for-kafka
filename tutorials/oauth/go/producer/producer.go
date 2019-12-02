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
	"time"

	"github.com/Azure/go-autorest/autorest/adal"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func handleOAuthBearerTokenRefreshEvent(client kafka.Handle, e kafka.OAuthBearerTokenRefresh, spt *adal.ServicePrincipalToken) {
	fmt.Println("handleOAuthBearerTokenRefreshEvent")
	oauthBearerToken, retrieveErr := retrieveToken(e, spt)
	if retrieveErr != nil {
		fmt.Fprintf(os.Stderr, "%% Token retrieval error: %v\n", retrieveErr)
		client.SetOAuthBearerTokenFailure(retrieveErr.Error())
	} else {
		setTokenError := client.SetOAuthBearerToken(*oauthBearerToken)
		if setTokenError != nil {
			fmt.Fprintf(os.Stderr, "%% Error setting token and extensions: %v\n", setTokenError)
			client.SetOAuthBearerTokenFailure(setTokenError.Error())
		}
	}
}

func retrieveToken(e kafka.OAuthBearerTokenRefresh, spt *adal.ServicePrincipalToken) (*kafka.OAuthBearerToken, error) {
	fmt.Println("in retrieveToken")

	// Acquire a new access token
	err = spt.Refresh()
	if err != nil {
		return nil, err
	}

	token := spt.OAuthToken()
	fmt.Println("Token " + token)

	now := time.Now()
	//owSecondsSinceEpoch := now.Unix()

	// The token lifetime needs to be long enough to allow connection and a broker metadata query.
	// We then exit immediately after that, so no additional token refreshes will occur.
	// Therefore set the lifetime to be an hour (though anything on the order of a minute or more
	// would be fine).
	expiration := now.Add(time.Second * time.Duration(3600))
	// expirationSecondsSinceEpoch := expiration.Unix()

	// oauthbearerMapForJSON := map[string]interface{}{
	// 	principalClaimName: principal,
	// 	"iat":              nowSecondsSinceEpoch,
	// 	"exp":              expirationSecondsSinceEpoch,
	// }
	// claimsJSON, _ := json.Marshal(oauthbearerMapForJSON)
	// encodedClaims := base64.RawURLEncoding.EncodeToString(claimsJSON)
	// jwsCompactSerialization := joseHeaderEncoded + "." + encodedClaims + "."
	extensions := map[string]string{}
	oauthBearerToken := kafka.OAuthBearerToken{
		TokenValue: "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6IkJCOENlRlZxeWFHckdOdWVoSklpTDRkZmp6dyIsImtpZCI6IkJCOENlRlZxeWFHckdOdWVoSklpTDRkZmp6dyJ9.eyJhdWQiOiJodHRwczovL2ludDdibjMwMDYtMy02YjFmNC0xNi5zZXJ2aWNlYnVzLmludDcud2luZG93cy1pbnQubmV0IiwiaXNzIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3LyIsImlhdCI6MTU3NTE4Mzk3OSwibmJmIjoxNTc1MTgzOTc5LCJleHAiOjE1NzUxODc4NzksImFpbyI6IjQyVmdZQ2o3ZXVmZDN4bVNhcWU0aENhWThNaC9Cd0E9IiwiYXBwaWQiOiI1MWNlNzk0My1hMWFhLTRiYjEtYjcwOC04ZjY2NjU2YTkyNDIiLCJhcHBpZGFjciI6IjEiLCJpZHAiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC83MmY5ODhiZi04NmYxLTQxYWYtOTFhYi0yZDdjZDAxMWRiNDcvIiwib2lkIjoiMzRlOTRmODktZjNlNy00NWU4LWFjZDQtZTFjNGEzMDQ1M2JmIiwic3ViIjoiMzRlOTRmODktZjNlNy00NWU4LWFjZDQtZTFjNGEzMDQ1M2JmIiwidGlkIjoiNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3IiwidXRpIjoiYkZGUEFsV1Nia2E4dXY4cEpiNEFBUSIsInZlciI6IjEuMCJ9.Ixhhy1Eou_YquwLQRr3VWTPZPMo9_gxH8EdCPSR9-3HkGvuV1W9Hv_KqO7-R-41eFLqroZeP2lPEjXcGGLsuprKmQnauZgRqod6sEAR-mw8W3Ldy6rIg_5JLtpUZJvqwGI2h6vGRPHUzsryvOVWZYBq34138KF7CAhPAn2qbaKFHsepqBiE7qUPVDPKJCtSvln3SRttWZWwWE3-VMzUSAoP0VmxU4B_redlGMWjmVIZHUTEhIlC5CFFjStk5bDVazT8CkcHnEcJoLzQCTCT8Xgxg0V5obfARPcfWXK2wmdpoAZiN1xy1FX5533O7Oiah2Mx-XLRawQCUnhzFManh6A",
		Expiration: expiration,
		Principal:  "",
		Extensions: extensions,
	}

	return &oauthBearerToken, nil
}

func getServicePrincipalToken() (*adal.ServicePrincipalToken, error) {
	tenantID := os.Getenv("AAD_TENANT_ID")
	applicationID := os.Getenv("AAD_APPLICATION_ID")
	applicationSecret := os.Getenv("AAD_APPLICATION_SECRET")
	audience := os.Getenv("AAD_AUDIENCE")

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
		audience,
		callback)

	return spt, err
}

func main() {
	config := kafka.ConfigMap{
		"bootstrap.servers": os.Getenv("KAFKA_EVENTHUB_ENDPOINT"),
		"security.protocol": "SASL_SSL",
		"sasl.mechanisms":   "OAUTHBEARER",
	}

	p, err := kafka.NewProducer(&config)

	if err != nil {
		panic(err)
	}

	spt, err := getServicePrincipalToken()

	if err != nil {
		panic(err)
	}

	// Token refresh events are posted on the Events channel, instructing
	// the application to refresh its token.
	// go func(eventsChan chan kafka.Event) {
	// 	for ev := range eventsChan {
	// 		oart, ok := ev.(kafka.OAuthBearerTokenRefresh)
	// 		if !ok {
	// 			// Ignore other event types
	// 			continue
	// 		}

	// 		handleOAuthBearerTokenRefreshEvent(p, oart)
	// 	}
	// }(p.Events())

	// Delivery report handler for produced messages
	go func(eventsChan chan kafka.Event) {
		for ev := range eventsChan {
			oart, ok := ev.(kafka.OAuthBearerTokenRefresh)
			if ok {
				handleOAuthBearerTokenRefreshEvent(p, oart, spt)
			}

			switch et := ev.(type) {
			case *kafka.Message:
				if et.TopicPartition.Error != nil {
					fmt.Printf("Delivery failed: %v\n", et.TopicPartition)
				} else {
					fmt.Printf("Delivered message to %v\n", et.TopicPartition)
				}
			}
		}
	}(p.Events())

	// Produce messages to topic (asynchronously)
	topic := "test"
	for _, word := range []string{"Welcome", "to", "the", "Kafka", "head", "on", "Azure", "EventHubs"} {
		fmt.Println("sending")
		p.Produce(&kafka.Message{
			TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
			Value:          []byte(word),
		}, nil)
	}

	// Wait for message deliveries
	p.Flush(60 * 1000)
}
