# Using Confluent's Schema Registry with Event Hubs for Apache Kafka Ecosystems

This document will walk you through integrating Schema Registry and Event Hubs.

## Prerequisites

To complete this walkthough, make sure you have the following prerequisites:

* [Java Development Kit (JDK) 1.8+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
    * On Ubuntu, run `apt-get install default-jdk` to install the JDK.
    * Be sure to set the JAVA_HOME environment variable to point to the folder where the JDK is installed.
* A schema registry instance. This can be hosted locally, on an Azure VM, or on Confluent Cloud. A thorough guide on setting up a schema registry can be found [here](https://docs.confluent.io/current/schema-registry/docs/schema_registry_tutorial.html).

## Overview

[Confluent's Schema Registry](https://docs.confluent.io/current/schema-registry/docs/index.html) provides the standard interface for Avro serialization and deserialization of Kafka messages. This guide is for users who want to use Avro-style schemas with Event Hubs for Kafka. 

### FQDN

For this sample, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Set up

### Create an Avro serialized/deserialized Kafka producer and consumer

Confluent has a great sample [here](https://github.com/confluentinc/examples/tree/5.1.2-post/clients/avro).

### Update your configuration

You'll need to update the following configurations:

* `bootstrap.servers`
* `schema.registry.url`
* The three SASL authentication configs as noted [here]()

(If you're using the Confluent samples mentioned above, the configuration changes below will work for both the producer and consumer)

```java
//Update these two configurations
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "mynamespace.servicebus.windows.net:9092");
props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://<YOUR.SCHEMA.REGISTRY.IP.ADDRESS>:8081");

//Add these three configurations
props.put("security.protocol", "SASL_SSL")
props.put("sasl.mechanism","PLAIN")
props.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX\");
```

## Send and receive

Run your application(s)! Sending and receiving Avro data with Azure Event Hubs is as easy as making those configuration changes to your existing application. Since the schema registry isn't hosted by Event Hubs, access to your schema registry should be  exactly the same -- the only difference is that you are now sending to a fully-managed Event Hub instead of your own Kafka cluster.