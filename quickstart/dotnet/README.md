# Send and Receive Messages in C# using Azure Event Hubs for Apache Kafka Ecosystems

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using an example producer and consumer written in C# using .NET Core 2.0. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

This sample is based on [Confluent's Apache Kafka .NET client](https://github.com/confluentinc/confluent-kafka-dotnet), modified for use with Event Hubs for Kafka.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

In addition:

* [Visual Studio 2017](https://visualstudio.microsoft.com/downloads/)
* [Git](https://www.git-scm.com/downloads)

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For this sample, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

```
Endpoint=sb://{YOUR.EVENTHUBS.FQDN}/;SharedAccessKeyName={SHARED.ACCESS.KEY.NAME};SharedAccessKey={SHARED.ACCESS.KEY}
```

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and open EventHubsForKafkaSample.sln in VS2017:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/quickstart/dotnet
./EventHubsForKafkaSample.sln
```

## Import Confluent's Kafka package

Use the NuGet Package Manager UI (or Package Manager Console) to install the Confluent.Kafka package. More detailed instructions can be found [here](https://github.com/confluentinc/confluent-kafka-dotnet#referencing). 

**This tutorial uses Confluent.Kafka v0.11 - using v1.0-beta will cause compilation errors.**

## Update App.config

Update the values of the `EH_FQDN` and `EH_CONNECTION_STRING` in `App.config` to direct the application to the Event Hubs Kafka endpoint with the correct authentication. Default values for the Event Hub/topic name (`test`) and consumer group (`$Default`) have been filled in, but feel free to change those as needed.

## Run the application

Run the application in VS2017 and watch it go! If the Kafka-enabled Event Hub has incoming events, then the consumer should now begin receiving events from the topic set in `App.config`. 

**NOTE**: Since Kafka's default for an unknown consumer group is to read from the *latest* offset, the first time you run the consumer will not receive any events from this application's producer. This is because the producer sends its messages before the consumer polls in this application. To remedy this, either run the application several times with the same consumer group (so that the consumer group's offset will be known to the broker), set Kafka's [`auto.offset.reset` consumer config](https://kafka.apache.org/documentation/#newconsumerconfigs) to `earliest`, or use an external producer and produce simultaneously.

## Troubleshooting

Still not working? Here are some helpful tips:

* Try turning on debugging using librdkafka's debug config: Open up `Worker.cs` and insert `{ "debug", "security,broker,protocol" }` in your configuration dictionaries (if the problem is specific to the producer or the consumer, just put it in that client's dictionary)
* Make sure your connection string is at the **namespace level** (if your connection string ends with "EntityPath=<SomeEventHubName>", then it is an EventHub level connection string and will not work)
* Make sure your topic name is the name of an Event Hub that exists in your namespace
* If it still doesn't work, feel free to open up an issue on this Github and we'll help as soon as we can!
