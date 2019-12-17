# Using Apache Spark with Azure Event Hubs for Apache Kafka Ecosystems

This tutorial demonstrates how to integrate Event Hubs, Spark, and Hive Warehouse using the Apache Spark SQL connector and the Hive Warehouse connector. 

Azure Event Hubs for Apache Kafka Ecosystems generally supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later; however, **connecting Spark with Event Hubs using the native Spark Kafka connector requires Apache Kafka v2.0+ and Apache Spark v2.4+.**

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

This tutorial uses HDI Spark + Hive.  Follow the guidance provided [here](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector) to set a Hive-ready Spark cluster.

To complete this tutorial, you will need -
* HDI Spark cluster, integrated with Hive - follow the guidance provided [here](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector) to set up your cluster correctly
* Kafka-enabled Event Hubs namespace with an existing Event Hub topic of any configuration

This tutorial was tested with the following versions - 
- Apache Spark `v2.4.0.3.1.2.1-1`
- Scala `v2.11.12`
- Spark SQL `v2.4.3`
- Kafka `v1.0.1`
- Hive Warehouse Connector `v1.0.0.3.1.2.1-1`

### FQDN

For these samples, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Write to EH Kafka

Writing to a Kafka-enabled Event Hub is easier than ever before. Just make sure to update the `BOOTSTRAP_SERVERS` and `EH_SASL` variables with the information from your EventHub namespace. Check out our [example Spark producer](./SparkStreamingProducer.scala) for the full sample.

We are currently using kafka with batch mode.

We can get some sample data if we SSH into the cluster.

```bash
wget -P /tmp/small_radio_json.json https://raw.githubusercontent.com/Azure/usql/master/Examples/Samples/Data/json/radiowebsite/small_radio_json.json
```

Use the following command to start `spark-shell` with the correct packages loaded - 

```bash
spark-shell --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3 org.apache.kafka:kafka_2.11:1.0.1
```

If executing on Spark from a Jupyter notebook, execute the following lines to set up the correct environment.
```jupyter
%%configure
{ "conf": {"spark.jars.packages": "org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3,org.apache.kafka:kafka_2.11:1.0.1" }}
```

Use Spark Streaming to batch write to Event hub.  Execute the code found in the [example producer](./SparkStreamingProducer.scala).  The following snippet demonstrates using RDDs to write to Event Hubs:

```scala
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

## EH-Spark-Hive Integration

This tutorial demonstrates the use of HWC's Stream-to-Stream functionality.

Start `spark-shell` with the following bash command - 

```sh
sudo spark-shell --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3 org.apache.kafka:kafka_2.11:1.0.1 --jars /PATH/TO/hive-warehouse-connector-assembly.jar
```

The default path to the most current HWC assembly JAR is `/usr/hdp/current/hive_warehouse_connector/hive-warehouse-connector-assembly-{VERSION}.jar`.

The [consumer example](./Spark2HiveConsumer.scala) can be run from `spark-shell` to both read from Event Hubs and write to your Hive cluster. It features the `writeStream` call that utilizes HWC's `STREAM_TO_STREAM` functionality: 

```scala
df.filter($"key".isNotNull)
  .withColumn("key", convertToInt(df("key")))
  .withColumn("value", convertToString(df("value")))
  .writeStream
  .format(STREAM_TO_STREAM)
  .option("database", HIVE_DB_NAME)
  .option("table", HIVE_TABLE_NAME)
  .option("metastoreUri",spark.conf.get("spark.datasource.hive.warehouse.metastoreUri"))
  .option("checkpointLocation",CHECKPOINT_LOCATION)
  .start()
```

View the [consumer example](./Spark2HiveConsumer.scala) for the full Scala consumer implementation.

*Note: your checkpoint location persists even after Event Hub topics are created and deleted.  If you observe fetch timeout exceptions at offsets that do not exist, you should ensure that your checkpoint file is cleared of stale offsets.*

## Further reading

* [Integrating Spark and Hive via Apache Hive Warehouse connector](https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector)
* [Structured streaming from Event Hubs using the native Event Hubs Spark connector](https://github.com/Azure/azure-event-hubs-spark/blob/master/docs/structured-streaming-eventhubs-integration.md)