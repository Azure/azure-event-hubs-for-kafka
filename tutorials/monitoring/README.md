# Monitoring librdkafka and Java clients of Azure Event Hubs for Apache Kafka Ecosystems

This tutorial will be showing how to monitor Kafka client applications for Azure Event Hubs by using some suitable techniques. The solutions mentioned here will highlight the different approaches for Java and librdkafka based clients. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later. 

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

To complete this walkthrough, make sure you have the following prerequisites:

- Go through [Azure Event Hubs for Kafka dotnet client samples](https://github.com/Azure/azure-event-hubs-for-kafka/tree/master/quickstart/dotnet)
- Read through  [Azure Event Hubs Metrics in Azure Monitor](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-metrics-azure-monitor)

### Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For this sample, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

```
Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX
```

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. *.servicebus.chinacloudapi.cn, *.servicebus.usgovcloudapi.net, or *.servicebus.cloudapi.de).

### Create an Application Insights Resource

In order to track the metrics in real-time on Azure Monitor, it is essential to have an Application Insights Resource created. Also note down the the Instrumentation Key of this resource. For instructions, refer to this article [Creating an Appliction Insights Resource](https://docs.microsoft.com/en-us/azure/azure-monitor/app/create-new-resource).
