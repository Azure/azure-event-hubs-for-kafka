# Send and Receive Messages in Go using Azure Event Hubs for Apache Kafka Ecosystem

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using an example producer and consumer written in Go. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

This sample is based on [Confluent's Apache Kafka Golang client](https://github.com/confluentinc/confluent-kafka-go), modified for use with Event Hubs for Kafka.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

In addition:

* [Go Tools Distribution](https://golang.org/doc/install)
* [Git](https://www.git-scm.com/downloads)
    * On Ubuntu, you can run `sudo apt-get install git` to install Git.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

Additionally, topics in Kafka map to Event Hub instances, so create an Event Hub instance called "test" that our samples can send and receive messages from.

### FQDN

For this sample, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and navigate to the `quickstart/go` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/quickstart/go
```

## Configuration

Define two environmental variables that specify the fully qualified domain name and port of the Kafka head of your Event Hub and its connection string.

```bash
$ export KAFKA_EVENTHUB_ENDPOINT="mynamespace.servicebus.windows.net:9093", //REPLACE
$ export KAFKA_EVENTHUB_CONNECTION_STRING="Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX" //REPLACE
```

## Producer

The producer sample demonstrates how to send messages to the Event Hubs service using the Kafka head.

You can run the sample via:

```bash
$ cd producer
$ go run producer.go
```

The producer will now begin sending events to the Kafka-enabled Event Hub on topic `test` and printing the events to stdout. If you would like to change the topic, change the topic variable in `producer.go`.

## Consumer

The consumer sample demonstrates how to receive messages from the Event Hubs service using the Kafka head.

You can run the sample via:

```bash
$ cd consumer
$ go run consumer.go
```

The consumer will now begin receiving events from the Kafka-enabled Event Hub on topic `test` and printing the events to stdout. If you would like to change the topic, change the topic variable in `consumer.go`.
