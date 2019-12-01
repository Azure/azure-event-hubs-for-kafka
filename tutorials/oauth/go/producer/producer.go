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

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func handleOAuthBearerTokenRefreshEvent(client kafka.Handle, e kafka.OAuthBearerTokenRefresh) {
	fmt.Println("handleOAuthBearerTokenRefreshEvent")
	oauthBearerToken, retrieveErr := retrieveToken(e)
	if retrieveErr != nil {
		fmt.Fprintf(os.Stderr, "%% Token retrieval error: %v\n", retrieveErr)
		client.SetOAuthBearerTokenFailure(retrieveErr.Error())
	} else {
		setTokenError := client.SetOAuthBearerToken(oauthBearerToken)
		if setTokenError != nil {
			fmt.Fprintf(os.Stderr, "%% Error setting token and extensions: %v\n", setTokenError)
			client.SetOAuthBearerTokenFailure(setTokenError.Error())
		}
	}
}

func retrieveToken(e kafka.OAuthBearerTokenRefresh) (kafka.OAuthBearerToken, error) {
	fmt.Println("in retrieveToken")

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
		TokenValue: "sdfsfsdf'",
		Expiration: expiration,
		Principal:  "principal",
		Extensions: extensions,
	}
	return oauthBearerToken, nil
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
	go func() {
		for e := range p.Events() {
			oart, ok := ev.(kafka.OAuthBearerTokenRefresh)
			if ok {
				handleOAuthBearerTokenRefreshEvent(p, oart)
			}

			switch ev := e.(type) {
			case *kafka.Message:
				if ev.TopicPartition.Error != nil {
					fmt.Printf("Delivery failed: %v\n", ev.TopicPartition)
				} else {
					fmt.Printf("Delivered message to %v\n", ev.TopicPartition)
				}
			}
		}
	}()

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
