# Using Apache Spark with Azure Event Hubs for Apache Kafka Ecosystems

This tutorial will show how to connect your Spark application to a Kafka-enabled Event Hub without changing your protocol clients or running your own Kafka clusters. Azure Event Hubs for Apache Kafka Ecosystems generally supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later; however, **connecting Spark with Event Hubs using the native Spark Kafka connector requires Apache Kafka v2.0+ and Apache Spark v2.4+.**

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

In addition:

* [Spark v2.4](https://spark.apache.org/downloads.html)
* [Git](https://www.git-scm.com/downloads)

### Version Requirements

The Spark-Kafka adapter was updated to support Kafka v2.0 as of Spark v2.4. In previous releases of Spark, the adapter "supported" Kafka v0.10 and later but relied specifically on Kafka v0.10 APIs. Since Event Hubs for Kafka Ecosystems does not support Kafka v0.10, **the Spark-Kafka adapters from versions of Spark prior to v2.4 is not supported by Event Hubs for Kafka Ecosystems.**

We recommend making the switch to Spark v2.4 to make use of the native Kafka connector. If that isn't feasible, check out the [EventHubs Spark connector](https://github.com/Azure/azure-event-hubs-spark) which supports Spark v2.1 and later.

## Running Spark

Running Spark for the first time can be overwhelming. If you don't already have Spark running in your own environment, we recommend using [Azure Databricks](https://azure.microsoft.com/services/databricks/) to simplify the process - it'll take care of the details so you can focus on your application. If you decide to go with Azure Databricks, make sure to use Runtime Version `5.0 (Scala 2.11)` (the first Databricks runtime that uses Spark v2.4) or later. Importing the Spark v2.4 connector JARs on a pre-5.0 runtime will not work.

Whether you end up choosing a cloud platform like Azure Databricks or decide to run on your on-prem cluster, Event Hubs for Kafka will work all the same.

*Note: Databricks shades the Kafka client under the `kafkashaded` package. If you are using Databricks to run Spark:

1. Do not import `org.apache.kafka.common.security.plain.PlainLoginModule` (it's provided by the Databricks runtime)
2. Update your EH_SASL constant's `org.apache.kafka.common.security.plain.PlainLoginModule` to `kafkashaded.org.apache.kafka.common.security.plain.PlainLoginModule`

## Microbatching vs Continuous Processing

Spark began as a purely microbatched system, but as of version 2.3, Spark has an experimental [`Continuous Mode`](https://databricks.com/blog/2018/03/20/low-latency-continuous-processing-mode-in-structured-streaming-in-apache-spark-2-3-0.html) to support continuous processing. Both microbatch and continuous processing are supported by EventHubs for Kafka, so feel free to pick whichever makes the most sense for your application.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For these samples, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs repository and navigate to the `tutorials/spark` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/spark
```

## Read from Event Hubs for Kafka

Reading from a Kafka-enabled Event Hub is as simple as setting a few extra configurations. Just make sure to update the `BOOTSTRAP_SERVERS` and `EH_SASL` variables with the information from your EventHub namespace. Check out our [example Spark consumer](./sparkConsumer.scala) for the full sample.

```scala
//Read from your Event Hub!
val df = spark.readStream
    .format("kafka")
    .option("subscribe", TOPIC)
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("kafka.sasl.mechanism", "PLAIN")
    .option("kafka.security.protocol", "SASL_SSL")
    .option("kafka.sasl.jaas.config", EH_SASL)
    .option("kafka.request.timeout.ms", "60000")
    .option("kafka.session.timeout.ms", "60000")
    .option("kafka.group.id", GROUP_ID)
    .option("failOnDataLoss", "false")
    .load()

//Use dataframe like normal (in this example, write to console)
val df_write = df.writeStream
    .outputMode("append")
    .format("console")
    .start()
```

## Write to Event Hubs for Kafka

Writing to a Kafka-enabled Event Hub is easier than ever before. Just make sure to update the `BOOTSTRAP_SERVERS` and `EH_SASL` variables with the information from your EventHub namespace. Check out our [example Spark producer](./sparkProducer.scala) for the full sample.

```scala
df = /**Dataframe**/

//Write to your Event Hub!
df.writeStream
    .format("kafka")
    .option("topic", TOPIC)
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("kafka.sasl.mechanism", "PLAIN")
    .option("kafka.security.protocol", "SASL_SSL")
    .option("kafka.sasl.jaas.config", EH_SASL)
    .option("checkpointLocation", "./checkpoint")
    .start()
```
