/**
 * Read from a Kafka-enabled Event Hub as EventHub format
 * sudo spark-shell --packages com.microsoft.azure:azure-eventhubs-spark_2.11:2.3.10 --jars /usr/hdp/current/hive_warehouse_connector/hive-warehouse-connector-assembly-1.0.0.3.1.2.1-1.jar
 * https://docs.microsoft.com/en-us/azure/hdinsight/interactive-query/apache-hive-warehouse-connector 
 * https://github.com/Azure/azure-event-hubs-spark/blob/master/docs/structured-streaming-eventhubs-integration.md
 * https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-spark-connector
 * https://github.com/Azure/azure-event-hubs-spark/blob/master/core/src/main/scala/org/apache/spark/eventhubs/EventPosition.scala
 */

//import Event Hub, Spark, and Hive
import org.apache.spark.eventhubs._
import com.hortonworks.hwc.HiveWarehouseSession
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.udf
import com.hortonworks.hwc.HiveWarehouseSession
import com.hortonworks.hwc.HiveWarehouseSession._

//helper method to convert data types
def toInt(s: String): Int = {
  try {
    s.toInt
  } catch {
    case e: Exception => 0
  }
}

//Update values as needed
val TOPIC = "spark-test"
val BOOTSTRAP_SERVERS = "<YOUR.EVENTHUB.FQDN>:9093"
val EH_CONNECTION_STRING = "<EH-CONNECTION-STRING>"
val GROUP_ID = "$Default"

//Make sure permissions are set for the checkpoint location.
val CHECKPOINT_LOCATION = "/tmp/checkpoint"
val HIVE_TABLE_NAME = "stream_table"
val HIVE_DB_NAME = "testdb"
//https://github.com/hortonworks/hive-warehouse-connector/blob/HDP-3.1.2.1/src/main/java/com/hortonworks/hwc/HiveWarehouseSession.java
val STREAM_TO_STREAM = "com.hortonworks.spark.sql.hive.llap.streaming.HiveStreamingDataSource"

val conf = new SparkConf()
conf.setAppName("HWC Test")
val spark = SparkSession.builder().config(conf).enableHiveSupport().getOrCreate()

//start here
import spark.implicits._
val hive = com.hortonworks.spark.sql.hive.llap.HiveWarehouseBuilder.session(spark).build()

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

val convertToString = udf((payload: Array[Byte]) => new String(payload))
val convertToInt = udf((payload: Array[Byte]) => toInt(new String(payload)))
hive.setDatabase(HIVE_DB_NAME)

hive.createTable(HIVE_TABLE_NAME)
  .ifNotExists()
  .column("key","int")
  .column("value","string")
  .create()

hive.table(HIVE_TABLE_NAME).show() //sanity check

//write stream to HWC.  Using org.apache.spark:spark-sql-kafka-0-10_2.11:2.4.3 org.apache.kafka:kafka_2.11:1.0.1 + hive-warehouse-connector-assembly-1.0.0.3.1.2.1-1.jar
eventhubs
    .filter($"key".isNotNull)
    .withColumn("key", convertToInt(eventhubs("key")))
    .withColumn("value", convertToString(eventhubs("value")))
    .writeStream
    .format(STREAM_TO_STREAM)
    .option("database", HIVE_DB_NAME)
    .option("table",HIVE_TABLE_NAME)
    .option("metastoreUri",spark.conf.get("spark.datasource.hive.warehouse.metastoreUri"))
    .option("checkpointLocation",CHECKPOINT_LOCATION)
    .start()
