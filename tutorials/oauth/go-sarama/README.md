# Send and Receive Messages in Go using Azure Event Hubs for Apache Kafka Ecosystem with OAuthBearer

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using an example producer and consumer written in Go. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

In addition:

* [Go Tools Distribution](https://golang.org/doc/install)
* [Git](https://www.git-scm.com/downloads)
    * On Ubuntu, you can run `sudo apt-get install git` to install Git.
* Go language
    * On Ubuntu, run `sudo apt-get install golang`.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

Additionally, topics in Kafka map to Event Hub instances, so create an Event Hub instance called "test" that our samples can send and receive messages from.

### FQDN

For these samples, you will need the Fully Qualified Domain Name of your Event Hubs namespace which can be found in Azure Portal. To do so, in Azure Portal, go to your Event Hubs namespace overview page and copy host name which should look like `**`mynamespace.servicebus.windows.net`**`.

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Create an Azure Active Directory Application

In order to run these samples, you will need to create an AAD application with a client secret and assign it as EventHubs Data Owner on the Event Hubs namespace you created in the previous section.

Learn more about [AAD Role Based Access Control](https://docs.microsoft.com/en-us/azure/role-based-access-control/overview)

Learn more about [Azure EventHubs Role Based Access Control](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory)

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and navigate to the `tutorials/oauth/go-sarama` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/oauth/go-sarama
```
    
Fetch the Sarama Kafka client library

    go get github.com/Shopify/sarama
    
## Configuration

Define these environmental variables that specify the fully qualified domain name and port of the Kafka head of your Event Hub and AAD application that will be used to acquire access token.

```bash
$ export KAFKA_EVENTHUB_ENDPOINT="mynamespace.servicebus.windows.net:9093" # REPLACE
$ export AAD_TENANT_ID="your AAD tenant-id" # REPLACE
$ export AAD_APPLICATION_ID="your AAD application id" # REPLACE
$ export AAD_APPLICATION_SECRET="your AAD application secret" # REPLACE
$ export AAD_AUDIENCE="https://mynamespace.servicebus.windows.net" # REPLACE
```

## Producer

The producer sample demonstrates how to send messages to the Event Hubs service using the Kafka head.

You can run the sample via:

```bash
$ cd producer
$ go run producer.go token-provider.go
```

The producer will now begin sending events to the Kafka-enabled Event Hub on topic `test` and printing the events to stdout. If you would like to change the topic, change the topic variable in `producer.go`.

## Consumer

The consumer sample demonstrates how to receive messages from the Event Hubs service using the Kafka head.

You can run the sample via:

```bash
$ cd consumer
$ go run consumer.go token-provider.go
```

The consumer will now begin receiving events from the Kafka-enabled Event Hub on topic `test` and printing the events to stdout. If you would like to change the topic, change the topic variable in `consumer.go`.

