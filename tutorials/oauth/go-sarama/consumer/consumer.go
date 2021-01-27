package main

import (
	"context"
	"crypto/tls"
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/Shopify/sarama"
)

func main() {
	brokerList := []string{os.Getenv("KAFKA_EVENTHUB_ENDPOINT")}
	fmt.Println("Event Hubs broker", brokerList)
	consumerGroupID := getEnv(consumerGroupIDEnvVar)
	fmt.Println("Sarama client consumer group ID", consumerGroupID)

	consumer, err := sarama.NewConsumerGroup(brokerList, consumerGroupID, getConfig())

	if err != nil {
		fmt.Println("error creating new consumer group", err)
		os.Exit(1)
	}

	fmt.Println("new consumer group created")

	eventHubsTopic := os.Getenv("EVENTHUBS_TOPIC")
	fmt.Println("Event Hubs topic", eventHubsTopic)

	ctx, cancel := context.WithCancel(context.Background())
	go func() {
		for {
			err = consumer.Consume(ctx, []string{getEnv(eventHubsTopicEnvVar)}, messageHandler{})
			if err != nil {
				fmt.Println("error consuming from group", err)
				os.Exit(1)
			}

			if ctx.Err() != nil {
				//exit for loop
				return
			}
		}
	}()

	close := make(chan os.Signal)
	signal.Notify(close, syscall.SIGTERM, syscall.SIGINT)
	fmt.Println("Waiting for program to exit")
	<-close
	cancel()
	fmt.Println("closing consumer group....")

	if err := consumer.Close(); err != nil {
		fmt.Println("error trying to close consumer", err)
		os.Exit(1)
	}
	fmt.Println("consumer group closed")
}

type messageHandler struct{}

func (h messageHandler) Setup(s sarama.ConsumerGroupSession) error {
	fmt.Println("Partition allocation -", s.Claims())
	return nil
}

func (h messageHandler) Cleanup(s sarama.ConsumerGroupSession) error {
	fmt.Println("Consumer group clean up initiated")
	return nil
}
func (h messageHandler) ConsumeClaim(s sarama.ConsumerGroupSession, c sarama.ConsumerGroupClaim) error {
	for msg := range c.Messages() {
		fmt.Printf("Message topic:%q partition:%d offset:%d\n", msg.Topic, msg.Partition, msg.Offset)
		fmt.Println("Message content", string(msg.Value))
		s.MarkMessage(msg, "")
	}
	return nil
}

func getConfig() *sarama.Config {
	config := sarama.NewConfig()
	config.Net.DialTimeout = 10 * time.Second
	config.Net.SASL.Enable = true
	config.Net.SASL.Mechanism = sarama.SASLTypeOAuth
	config.Net.SASL.TokenProvider = NewTokenProvider()
	config.Net.TLS.Enable = true
	config.Version = sarama.V1_0_0_0
	config.Net.TLS.Config = &tls.Config{
		InsecureSkipVerify: true,
		ClientAuth:         0,
	}

	return config
}

func getEnv(envName string) string {
	value := os.Getenv(envName)
	if value == "" {
		fmt.Println("Environment variable " + envName + " is missing")
		os.Exit(1)
	}
	return value
}
