//Copyright (c) Microsoft Corporation. All rights reserved.
//Copyright 2016 Confluent Inc.
//Licensed under the MIT License.
//Licensed under the Apache License, Version 2.0
//
//Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/Azure/go-autorest/autorest/adal"
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

// Claims is the main container for our body information
type Claims map[string]interface{}

func getClaimsFromJwt(tokenStr string) (Claims, error) {
	tokenArray := strings.Split(tokenStr, ".")

	claimsByte, err := base64.RawURLEncoding.DecodeString(tokenArray[1])
	if err != nil {
		return nil, err
	}

	var claims Claims
	err = json.Unmarshal(claimsByte, &claims)
	if err != nil {
		return nil, err
	}

	return claims, nil
}

func getExpirationFromClaims(claims Claims) time.Time {
	if obj, ok := claims["exp"]; ok {
		if expVal, ok := obj.(float64); ok {
			return time.Unix(int64(expVal), 0)
		}
	}

	return time.Now()
}

func handleOAuthBearerTokenRefreshEvent(client kafka.Handle, e kafka.OAuthBearerTokenRefresh, spt *adal.ServicePrincipalToken) {
	fmt.Printf("handleOAuthBearerTokenRefreshEvent")
	oauthBearerToken, err := retrieveToken(e, spt)
	if err != nil {
		fmt.Fprintf(os.Stderr, "%% Token retrieval error: %v\n", err)
		client.SetOAuthBearerTokenFailure(err.Error())
	} else {
		setTokenError := client.SetOAuthBearerToken(*oauthBearerToken)
		if setTokenError != nil {
			fmt.Fprintf(os.Stderr, "%% Error setting token and extensions: %v\n", setTokenError)
			client.SetOAuthBearerTokenFailure(setTokenError.Error())
		}
	}
}

func retrieveToken(e kafka.OAuthBearerTokenRefresh, spt *adal.ServicePrincipalToken) (*kafka.OAuthBearerToken, error) {
	extensions := map[string]string{}

	// Acquire a new token and extract expiry
	err := spt.Refresh()
	if err != nil {
		return nil, err
	}

	tokenString := spt.OAuthToken()
	claims, _ := getClaimsFromJwt(tokenString)
	expiration := getExpirationFromClaims(claims)

	oauthBearerToken := kafka.OAuthBearerToken{
		TokenValue: tokenString,
		Expiration: expiration,
		Principal:  "",
		Extensions: extensions,
	}

	fmt.Println(tokenString)

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
	consumerGroup := "consumergroup"

	config := kafka.ConfigMap{
		"bootstrap.servers": os.Getenv("KAFKA_EVENTHUB_ENDPOINT"),
		"security.protocol": "SASL_SSL",
		"sasl.mechanisms":   "OAUTHBEARER",
		"group.id":          consumerGroup,
		"auto.offset.reset": "earliest",
		"debug":             "consumer",
	}

	c, err := kafka.NewConsumer(&config)

	if err != nil {
		panic(err)
	}

	spt, err := getServicePrincipalToken()

	if err != nil {
		panic(err)
	}

	topics := []string{"test"}
	c.SubscribeTopics(topics, nil)

	// Event handler for produced messages and token refresh
	go func(eventsChan chan kafka.Event) {
		for ev := range eventsChan {
			fmt.Println("ev.(kafka.OAuthBearerTokenRefresh)")
			oart, ok := ev.(kafka.OAuthBearerTokenRefresh)
			if ok {
				fmt.Println("calling handleOAuthBearerTokenRefreshEvent")
				handleOAuthBearerTokenRefreshEvent(c, oart, spt)
			}
		}
	}(c.Events())

	for {
		msg, err := c.ReadMessage(-1)
		if err == nil {
			fmt.Printf("Message on %s: %s\n", msg.TopicPartition, string(msg.Value))
		} else {
			fmt.Printf("Consumer error: %v (%v)\n", err, msg)
			break
		}
	}

	c.Close()
}
