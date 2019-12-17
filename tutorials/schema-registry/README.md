# Using Confluent's Schema Registry with Event Hubs for Apache Kafka Ecosystems

This tutorial will walk you through integrating Schema Registry and Event Hubs for Kafka.

## Prerequisites

To complete this walkthough, make sure you have the following prerequisites:

* An Avro serialized/deserialized producer and consumer - you can find a tutorial [here](https://github.com/confluentinc/examples/tree/5.1.2-post/clients/avro).
* A schema registry instance running on top of an Apache Kafka metadata store. The cluster and schema registry instance can be hosted locally, on an Azure VM, or on Confluent Cloud. A thorough guide on setting up a schema registry can be found [here](https://docs.confluent.io/current/schema-registry/docs/schema_registry_tutorial.html).

## Overview

[Confluent's Schema Registry](https://docs.confluent.io/current/schema-registry/docs/index.html) provides the standard interface for Avro serialization and deserialization of Kafka messages. This guide is for users who want to use those Avro-style schemas with Event Hubs for Kafka. 

There are two potential uses for Event Hubs in the Schema Registry scenario because there are two logically separate stores. The first is the metadata store (`schema.registry.url`) that the schema registry stores its schemas in. The second is the data store which is used for the standard sending/receiving of data (`bootstrap.servers`). Often applications will put both of these in the same Kafka cluster to simplify the design and make maintenance easier, but they can also be run separately. For the metadata store, Schema Registry requires AdminClient APIs that Event Hubs for Kafka does not currently support; however, it does not require these APIs of the data store.

Because of this distinction, Event Hubs for Kafka can currently serve as the data store but *not* the metadata store (i.e. a separate Kafka cluster is required for the metadata store). This walkthrough requires a separate Apache Kafka cluster as the backing metadata store for the schema registry, and is not a full solution for many scenarios. In the future, Event Hubs for Kafka will be able to be used as *both* the backing metadata store and data store, and this walkthrough will be updated at that time.

### FQDN

For this sample, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Update your configuration

You'll need to update the following two configurations:

* `bootstrap.servers` (should point to your Event Hubs for Kakfa namespace)
* `schema.registry.url` (should point to your separate Kafka cluster)

Along with these three SASL authentication configs as noted [here](../README.md#update-your-kafka-client-configuration):

* `sasl.mechanism`  
* `security.protocol`
* `sasl.jaas.config`

(If you're using the Confluent sample mentioned above, the configuration changes below should do the trick for both producer and consumer)

```java
//Configuration updates for a Kafka client using Avro serialization/deserialization with Event Hubs for Kafka

//Update these configs
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "mynamespace.servicebus.windows.net:9093");
props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://<YOUR.SCHEMA.REGISTRY.IP.ADDRESS>:8081");

//Add these configs
props.put("security.protocol", "SASL_SSL");
props.put("sasl.mechanism","PLAIN");
props.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"$ConnectionString\" password=\"Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX\";");
```

## Send and receive

Run your application(s)! Sending and receiving Avro data with Azure Event Hubs is as easy as making those configuration changes to your existing application. Since the schema registry isn't hosted by Event Hubs, access to your schema registry should be  exactly the same -- the only difference is that you are now sending your data to a fully-managed Event Hub instead of your own Kafka cluster.
