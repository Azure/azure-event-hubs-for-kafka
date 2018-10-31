/**
 * Read from a Kafka-enabled Event Hub
 */

//Import login module
import org.apache.kafka.common.security.plain.PlainLoginModule

//Update values as needed
val TOPIC = "test"
val BOOTSTRAP_SERVERS = "<YOUR.EVENTHUB.FQDN>:9093"
val EH_SASL = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"<YOUR.EVENTHUB.CONNECTION.STRING>\";"
val GROUP_ID = "$Default"

//Read stream using Spark's Kafka connector
val df = spark.readStream
    .format("kafka")
    .option("subscribe", TOPIC)
    .option("kafka.bootstrap.servers", BOOTSTRAP_SERVERS)
    .option("kafka.sasl.mechanism", "PLAIN")
    .option("kafka.security.protocol", "SASL_SSL")
    .option("kafka.sasl.jaas.config", EH_SASL)
    .option("kafka.request.timeout.ms", "60000")
    .option("kafka.session.timeout.ms", "30000")
    .option("kafka.group.id", GROUP_ID)
    .option("failOnDataLoss", "false")
    .load()

//Use DataFrame like normal. In this example, do no processing and write to console.
dfWrite.writeStream
    .outputMode("append")
    .format("console")
    .start()