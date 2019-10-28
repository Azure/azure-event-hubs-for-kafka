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
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func main() {
	consumerGroup := "consumergroup"
	
	https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md
	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		"bootstrap.servers": os.Getenv("KAFKA_EVENTHUB_ENDPOINT"),
		"sasl.mechanisms":   "PLAIN",
		"security.protocol": "SASL_SSL",
		"sasl.username":     "$ConnectionString",
		"sasl.password":     os.Getenv("KAFKA_EVENTHUB_CONNECTION_STRING"),
		"group.id":          consumerGroup,
		"auto.offset.reset": "earliest",
		"debug":	     "consumer",
	})

	if err != nil {
		panic(err)
	}

	topics := []string{"test"}
	c.SubscribeTopics(topics, nil)

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
