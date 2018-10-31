/**
 * Write to a Kafka-enabled Event Hub
 */

//Import login module
import org.apache.kafka.common.security.plain.PlainLoginModule

//Update values as needed
val TOPIC = "test"
val BOOTSTRAP_SERVERS = "<YOUR.EVENTHUB.FQDN>:9093"
val EH_SASL = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"<YOUR.EVENTHUB.CONNECTION.STRING>\";"

//Define dataframe
df = /**Dataframe**/

//Write df to EventHubs using Spark's Kafka connector
df.writeStream
    .format("kafka")
    .option("topic", TOPIC)
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("kafka.sasl.mechanism", "PLAIN")
    .option("kafka.security.protocol", "SASL_SSL")
    .option("kafka.sasl.jaas.config", EH_SASL)
    .option("checkpointLocation", "./checkpoint")
    .start()