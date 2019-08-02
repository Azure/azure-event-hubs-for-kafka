# Using Apache Spark with Azure Event Hubs for Apache Kafka Ecosystems

This tutorial will show how to connect your Spark application to a Kafka-enabled Event Hub.  We will assume that the Spark application will also write to Hive with Hive Warehouse Connector [in a separate set up](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector). 

Azure Event Hubs for Apache Kafka Ecosystems generally supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later; however, **connecting Spark with Event Hubs using the native Spark Kafka connector requires Apache Kafka v2.0+ and Apache Spark v2.4+.**

## Helpful Reading

Here's some guidance around using HDInsight with Spark and HDInsight Hive, and also Event Hub.

* [HDI Spark to HDI Hive](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector)
* [Hive Warehouse Connector](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector)
* [Event Hub Streaming](https://github.com/Azure/azure-event-hubs-spark/blob/master/docs/structured-streaming-eventhubs-integration.md)
* [Event Hub Spark Connector](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-spark-connector)
* [Event Hub Position](https://github.com/Azure/azure-event-hubs-spark/blob/master/core/src/main/scala/org/apache/spark/eventhubs/EventPosition.scala)

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

*Note: Databricks shades the Kafka client under the `kafkashaded` package. If you are using Databricks to run Spark, make sure to update all occurrences of `org.apache.kafka.common.security.plain.PlainLoginModule` to `kafkashaded.org.apache.kafka.common.security.plain.PlainLoginModule` in these samples!*

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
## Write to Event Hubs for Kafka

Writing to a Kafka-enabled Event Hub is easier than ever before. Just make sure to update the `BOOTSTRAP_SERVERS` and `EH_SASL` variables with the information from your EventHub namespace. Check out our [example Spark producer](./sparkProducer.scala) for the full sample.

We are currently using kafka with batch mode.

We can get some sample data if we SSH into the cluster.

```bash
wget -P /tmp/small_radio_json.json https://raw.githubusercontent.com/Azure/usql/master/Examples/Samples/Data/json/radiowebsite/small_radio_json.json
```

In our test, we used spark-shell on the HDI Spark cluster.  We can also run this from Spark in HDI Spark from a Jupyter notebook.

```bash
spark-shell --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3 org.apache.kafka:kafka_2.11:1.0.1
```

If we're running this from Spark in a Jupyter notebook, we can use the following cell magic to set up the environment.
```jupyter
%%configure
{ "conf": {"spark.jars.packages": "org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3,org.apache.kafka:kafka_2.11:1.0.1" }}
```

We can attempt to use the kafka protocol to batch write to Event hub.

```scala
//Sample data can be found here: wget -P /tmp/small_radio_json.json https://raw.githubusercontent.com/Azure/usql/master/Examples/Samples/Data/json/radiowebsite/small_radio_json.json
val df = spark
    .read
    .schema(artistschema)
    .json("/tmp/small_radio_json.json")
val dfWrite = df.selectExpr("CAST(userId as STRING) as key", "to_json(struct(*)) AS value")
dfWrite.show()
//Write to your Event Hub as batch
dfWrite.write
    .format("kafka")
    .option("topic", TOPIC)
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("kafka.sasl.mechanism", "PLAIN")
    .option("kafka.security.protocol", "SASL_SSL")
    .option("kafka.sasl.jaas.config", EH_SASL)
    .option("checkpointLocation", CHECKPOINT_PATH)
    .save()
```

## Read from Event Hubs and Write to Hive

There's a couple options for writing to hive from Event hub in Spark.  Note that these are still **pending additional testing**.

* Use Kafka Connector in Spark
* Use Event Hub Connector in Spark

Be sure to also work through setting up Hive with Spark.
* [HDI Spark to HDI Hive](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector)
* [Hive Warehouse Connector](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector)

### Using Kafka Connector in Spark

Writing to a Kafka-enabled Event Hub is easier than ever before. Just make sure to update the `BOOTSTRAP_SERVERS` and `EH_SASL` variables with the information from your EventHub namespace. Check out our [example Spark consumer](./sparkConsumerKafka.scala) for the full sample.

From Spark-Shell, we can test with the following command to load the environment.

```
sudo spark-shell --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3 org.apache.kafka:kafka_2.11:1.0.1 --jars /path/to/hive-warehouse-connector-assembly.jar
```

In our case, the path to the hive warehouse connector assembly was /usr/hdp/current/hive_warehouse_connector/hive-warehouse-connector-assembly-1.0.0.3.1.2.1-1.jar.

```scala
//helper method to convert data types
def toInt(s: String): Int = {
  try {
    s.toInt
  } catch {
    case e: Exception => 0
  }
}

val df = spark.read
    .format("kafka")
    .option("subscribe", TOPIC)
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("kafka.sasl.mechanism", "PLAIN")
    .option("kafka.security.protocol", "SASL_SSL")
    .option("kafka.sasl.jaas.config", EH_SASL)
    .option("kafka.request.timeout.ms", "60000")
    .option("kafka.session.timeout.ms", "60000")
    .option("failOnDataLoss", "false")
    .load()

hive.setDatabase(HIVE_DB_NAME)

hive.createTable(HIVE_TABLE_NAME)
  .ifNotExists()
  .column("key","int")
  .column("value","string")
  .create()

hive.table(HIVE_TABLE_NAME).show() //sanity check

val convertToString = udf((payload: Array[Byte]) => new String(payload))
val convertToInt = udf((payload: Array[Byte]) => toInt(new String(payload)))

//Write to your Event Hub!
df.filter($"key".isNotNull)
    .withColumn("key", convertToInt(df("key")))
    .withColumn("value", convertToString(df("value")))
    .select("key","value")
    .write
    .format("com.hortonworks.spark.sql.hive.llap.HiveStreamingDataSource")
    .mode(SaveMode.Append)
    .option("table", "stream_table_2")
    .save()
```

### Using Event Hub Connector in Spark

We are going to use the **event hub** protocol to read from the event hub we just wrote to with **kafka** protocol.

Please refer to this [Event Hub Spark Connector Documentation](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-spark-connector) for additional details.

Check out our [example Spark consumer](./sparkConsumerEH.scala) for the full sample.

```scala
// To connect to an event hub, EntityPath is required as part of the connection string.
// Here, we assume that the connection string from the Azure portal does not have the EntityPath part.
val connectionString = ConnectionStringBuilder(EH_CONNECTION_STRING)
    .setEventHubName(TOPIC).build 
//testing from end of stream
val ehConf = EventHubsConf(connectionString)
    .setStartingPosition(EventPosition.fromEndOfStream)

// Create a stream that reads data from the specified Event Hub.
val reader = spark
    .readStream
    .format("eventhubs")
    .options(ehConf.toMap)
    .load()

val eventhubs = reader.select($"offset" cast "string" as "key", $"body" cast "string" as "value")

hive.setDatabase(HIVE_DB_NAME)

hive.createTable(HIVE_TABLE_NAME)
  .ifNotExists()
  .column("key","int")
  .column("value","string")
  .create()

hive.table(HIVE_TABLE_NAME).show() //sanity check

val convertToString = udf((payload: Array[Byte]) => new String(payload))
val convertToInt = udf((payload: Array[Byte]) => toInt(new String(payload)))

//Write from Event Hub to Hive.
eventhubs.filter($"key".isNotNull)
    .withColumn("key", convertToInt(eventhubs("key")))
    .withColumn("value", convertToString(eventhubs("value")))
    .writeStream
    .format(STREAM_TO_STREAM)
    .option("database", HIVE_DB_NAME)
    .option("table",HIVE_TABLE_NAME)
    .option("metastoreUri",spark.conf.get("spark.datasource.hive.warehouse.metastoreUri"))
    .option("checkpointLocation",CHECKPOINT_LOCATION)
    .start()
```
