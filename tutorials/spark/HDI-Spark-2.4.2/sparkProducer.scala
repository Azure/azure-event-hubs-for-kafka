/**
 * Write to a Kafka-enabled Event Hub
 * We can write this from spark (in jupyter or spark-shell for example)
 */

import org.apache.kafka.common.security.plain.PlainLoginModule
import org.apache.spark.sql.types.{StructType,StructField,StringType,IntegerType};

//Update values as needed
val TOPIC = "spark-test"
val BOOTSTRAP_SERVERS = "mynamespace.servicebus.windows.net:9093"
val EH_SASL = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX\";"
val CHECKPOINT_PATH = "./checkpoint"

//Define dataframe from HDI sample HVAC information
val artistschema = StructType(List(
    StructField("artist", StringType, true),
    StructField("firstName", StringType, true),
    StructField("lastName", StringType, true),
    StructField("location", StringType, true),
    StructField("song", StringType, true),
    StructField("userId", StringType, true)))

//Sample data can be found here: wget -P /tmp/small_radio_json.json https://raw.githubusercontent.com/Azure/usql/master/Examples/Samples/Data/json/radiowebsite/small_radio_json.json
val df = (spark
    .read
    .schema(artistschema)
    .json("/tmp/small_radio_json.json"))
val dfWrite = df.selectExpr("CAST(userId as STRING) as key", "to_json(struct(*)) AS value")
dfWrite.show()

//write as batch
(dfWrite.write
    .format("kafka")
    .option("topic", TOPIC)
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("kafka.sasl.mechanism", "PLAIN")
    .option("kafka.security.protocol", "SASL_SSL")
    .option("kafka.sasl.jaas.config", EH_SASL)
    .option("checkpointLocation", CHECKPOINT_PATH)
    .save())
