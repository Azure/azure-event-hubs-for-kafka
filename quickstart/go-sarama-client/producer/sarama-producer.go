package main

import (
	"crypto/tls"
	"fmt"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/Shopify/sarama"
)

const eventHubsConnStringEnvVar = "EVENTHUBS_CONNECTION_STRING"
const eventHubsBrokerEnvVar = "EVENTHUBS_BROKER"
const eventHubsTopicEnvVar = "EVENTHUBS_TOPIC"

func main() {
	brokerList := []string{getEnv(eventHubsBrokerEnvVar)}
	fmt.Println("Event Hubs broker", brokerList)

	producer, err := sarama.NewSyncProducer(brokerList, getConfig())
	if err != nil {
		fmt.Println("Failed to start Sarama producer:", err)
		os.Exit(1)
	}

	eventHubsTopic := getEnv(eventHubsTopicEnvVar)
	fmt.Println("Event Hubs topic", eventHubsTopic)
	producerOpen := true
	go func() {
		for {
			if producerOpen {
				ts := time.Now().String()
				msg := &sarama.ProducerMessage{Topic: eventHubsTopic, Key: sarama.StringEncoder("key-" + ts), Value: sarama.StringEncoder("value-" + ts)}
				p, o, err := producer.SendMessage(msg)
				if err != nil {
					fmt.Println("Failed to send msg:", err)
					continue
				}
				fmt.Printf("sent message to partition %d offset %d\n", p, o)
			}
			time.Sleep(3 * time.Second) //intentional pause
		}
	}()

	close := make(chan os.Signal)
	signal.Notify(close, syscall.SIGTERM, syscall.SIGINT)
	fmt.Println("Waiting for program to exit...")
	<-close

	fmt.Println("closing producer")
	err = producer.Close()
	producerOpen = false
	if err != nil {
		fmt.Println("failed to close producer", err)
	}
	fmt.Println("closed producer")
}
func getConfig() *sarama.Config {
	config := sarama.NewConfig()
	config.Net.DialTimeout = 10 * time.Second

	config.Net.SASL.Enable = true
	config.Net.SASL.User = "$ConnectionString"
	config.Net.SASL.Password = getEnv(eventHubsConnStringEnvVar)
	config.Net.SASL.Mechanism = "PLAIN"

	config.Net.TLS.Enable = true
	config.Net.TLS.Config = &tls.Config{
		InsecureSkipVerify: true,
		ClientAuth:         0,
	}
	config.Version = sarama.V1_0_0_0
	config.Producer.Return.Successes = true
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
