# Configuring kafkacat

*kafkacat* is a non-JVM command-line consumer and producer based on librdkafka, popular due to its speed and small footprint. This quickstart contains a sample configuration and several simple sample kafkacat commands. 

> Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https:/kafka.apache.org/10/documentation.html) and later.

## Prerequisites

- Create an [Azure Event Hubs namespace](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-create)
- Install kafkacat (Github repository located [here](https://github.com/edenhill/kafkacat))

*Note: this tutorial has only been tested on MacOS and Linux environment.*

## Using kafkacat

Your kafkacat configuration should include the following properties:
```properties
metadata.broker.list=mynamespace.servicebus.windows.net:9093
security.protocol=SASL_SSL
sasl.mechanisms=PLAIN
sasl.username=$ConnectionString
sasl.password=Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX
# Replace
# - 'metadata.broker.list' with your namespace FQDN (change 'mynamespace' to your namespace name)
# - 'sasl.password' with your namespace's connection string 
```

There are multiple ways to configure kafkacat as demonstrated in the kafkacat repository README [configuration section](https://github.com/edenhill/kafkacat#configuration).  This tutorial assumes the `$KAFKACAT_CONFIG` environment variable has been set to the absolute path of your kafkacat configuration file. Now go ahead and try out kafkacat!

The following command lists the topics in your currently referenced namespace:

```sh
kafkacat -b mynamespace.servicebus.windows.net:9093 -L
```

The following command prints all messages from the first available offset to console:

```sh
kafkacat -b mynamespace.servicebus.windows.net:9093 -t topic1 -o beginning
```

The following command uses standard high-level Kafka consumer for balancing and offset commits:

```sh
kafkacat -b mynamespace.servicebus.windows.net:9093 -G mygroup topic1 topic2
```

The kafkacat Github repository contains documentation for further commands.

For consumption - please note that consumption modes -C (low-level consumer) and -G (high-level consumer) are mutually exclusive and kafkacat will use the *last* mode flag in the command.  Also note that if you intend to use Kafka group semantics, you must specify the -G flag; setting group ID in your configuration file is not adequate.
