# Monitoring librdkafka based .NET clients of Azure Event Hubs for Apache Kafka Ecosystems

This tutorial will be showing how to monitor a librdkafka based .NET client application of Azure Event Hubs by using some suitable techniques. The approaches mentioned here will highlight the code changes to be made in order to track significant client metrics using Azure Monitor's Application Insights. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later. 

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

To complete this walkthrough, make sure you have the following prerequisites:

- .NET Core 3.0 or higher 

- Go through [Azure Event Hubs for Kafka dotnet client samples](https://github.com/Azure/azure-event-hubs-for-kafka/tree/master/quickstart/dotnet)
- Read [Azure Event Hubs Metrics in Azure Monitor](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-metrics-azure-monitor)

### Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For this sample, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

```
Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX
```

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. *.servicebus.chinacloudapi.cn, *.servicebus.usgovcloudapi.net, or *.servicebus.cloudapi.de).

### Create an Application Insights Resource

In order to track the metrics in real-time on Azure Monitor, it is essential to have an Application Insights Resource created. Also note down the the Instrumentation Key of this resource. For instructions, refer to this article: [Creating an Appliction Insights Resource](https://docs.microsoft.com/en-us/azure/azure-monitor/app/create-new-resource).

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string and an Application Insights Resource, clone the Azure Event Hubs for Kafka repository and navigate to the `tutorials/monitoring/librdkafka/dotnet` subdirectory:

```
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/monitoring/librdkafka/dotnet
```

You can now navigate to the consumer or producer directory and open the corresponding .sln file in VS or run the sample after making all the config changes by:

```
dotnet run --project consumer/ConsumerLaglibrdkafka/ConsumerLaglibrdkafka.csproj
dotnet run --project producer/ProducerMetricsLibrdkafka/ProducerMetricsLibrdkafka.csproj
```

## Overview 

For client configuration, refer to this sample:  [Azure Event Hubs for Kafka dotnet client samples](https://github.com/Azure/azure-event-hubs-for-kafka/tree/master/quickstart/dotnet).

The below notes touch upon the approach used for monitoring a consumer application. A similar approach can be used for producer applications as well. Librdkafka exposes its metrics in a JSON format. One of the metric parameters is consumer_lag (used as an example here for illustration purposes). As a proposed solution, the consumer_lag metric can be extracted from this JSON while the consumer application is consuming messages.

A format like the one below depicts how librdkafka exposes its metrics:

```json
{ 
    "topics": { 
        "random-evh-topic": { 
            "partitions": { 
                "0": { 
                    "consumer_lag": 4200, 
                } 
            } 
        } 
    } 
} 
```

Extract the consumer_lag information from the JSON data by making use some kind of suitable deserializer. 

In order to enable librdkafka to emit the statistics, a particular configuration needs to be enabled for librdkafka before creating the consumer instance. In the code sample, this is done in `ConsumerConfig`. Set `StatisticsIntervalMs = 1000`. `StatisticsIntervalMs` is the librdkafka statistics emit interval. The granularity is 1000ms. Providing a value of 0 will disable it (default). However, it should be noted that providing a value of 1000ms can cause the client to resend metric data in short intervals of time even if there is no significant change in the value.

The below suggested code changes are for latest versions. The proposed one is Confluent.Kafka version 1.5.1. The older versions have different ways of getting the statistics from the librdkafka. In order to extract the statistics from librdkafka, we need to set a particular handler, namely the StatisticsHandler, by calling the `SetStatisticsHandler()` on the `ConsumerBuilder` object as shown below:

```c#
var c = new ConsumerBuilder<long, string>(conf)
        .SetKeyDeserializer(Deserializers.Int64).SetValueDeserializer(Deserializers.Utf8)
        .SetErrorHandler((_, e) => Console.WriteLine($"Error:{e.Reason}"))
        .SetStatisticsHandler((_, json) => 
         { 
             var statistics = JsonConvert.DeserializeObject<ConsumerStatistics>(json); 
             foreach (var topic in statistics.Topics) 
             { 
                 foreach (var partition in topic.Value.Partitions) 
                 { 
                     //code for obtaining consumer_lag from partition and tracking using the Application Insights API 
                 } 
             } 
         }).Build()) 
```

The following significant metrics are tracked in the sample for monitoring the client throughput and performance:

| Consumer metrics | Producer metrics     |
| ---------------- | -------------------- |
| consumer-lag     | transmitted-requests |
| round-trip-time  | transmitted-bytes    |
|                  | transmitted-errors   |
|                  | request-retries      |
|                  | request-timeouts     |

The metrics shown above are present either at the partition level or at the broker level. For detailed information, refer [here](https://github.com/edenhill/librdkafka/blob/master/STATISTICS.md).

##### Partition ID -1 in JSON

A partition ID of -1 in the Statistics JSON signifies that it is an Internal UnAssigned (UA) partition. A UA partition in Confluent Kafka is a partition that no longer has a consumer, either because it is just created or its consumer no longer exists and needs new assignment.

#### Tracking Custom Metrics using Application Insights API

This sample uses Application Insights SDK for Worker Service applications. In order to get a better understanding of its use cases and template, refer [here](https://docs.microsoft.com/en-us/azure/azure-monitor/app/worker-service). Provide the Instrumentation Key of the resource in `appsettings.json`. Application insights API provides a way to track custom events or metrics in real time. Use the link in the references to get a better understanding of this technique. Initialize a `telemetryClient` instance and obtain the desired metric from the deserialized data and track using the `TrackMetric()` method as shown below:

```c#
IServiceProvider serviceProvider = services.BuildServiceProvider(); 
telemetryClient = serviceProvider.GetRequiredService<TelemetryClient>(); 
***
var consumer_lag = new MetricTelemetry(); 
consumer_lag.Name = "consumer lag"; 
consumer_lag.Sum = partition.Value.ConsumerLag; 
***
telemetryClient.TrackMetric(consumer_lag); 
```

The metric will be sent to the configured Application Insights Resource. It can be viewed under: Application Insights Resource -> Monitoring -> Metrics -> Custom [azure.application.insights]. 

![Visualization of Consumer Lag Metric](https://user-images.githubusercontent.com/73145416/97198367-18da9700-1785-11eb-93ce-e749522e9cbc.png)

## References

The below links can be used as quick references:

- [Statistics JSON structure for librdkafka](https://github.com/edenhill/librdkafka/blob/master/STATISTICS.md)
- [Confluent Kafka dotnet Consumer sample for handling statistics for confluent-kafka-dotnet v1.5.1](https://github.com/confluentinc/confluent-kafka-dotnet/blob/master/examples/Consumer/Program.cs)
- [API doc for ClientConfig in confluent-kafka-dotnet](https://docs.confluent.io/current/clients/confluent-kafka-dotnet/api/Confluent.Kafka.ClientConfig.html#Confluent_Kafka_ClientConfig_StatisticsIntervalMs)
- [Tracking Custom Metrics using Application Insights API](https://docs.microsoft.com/en-us/azure/azure-monitor/app/api-custom-events-metrics)
- [Application Insights SDK for Worker Service Applications](https://docs.microsoft.com/en-us/azure/azure-monitor/app/worker-service)