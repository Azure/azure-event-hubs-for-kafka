<p align="center">
  <img src="event-hubs.png" alt="Microsoft Azure Event Hubs" width="100"/>
</p>

<h1 align="center">Microsoft Azure Event Hubs
<p align="center">
  <a href="#star-our-repo">
        <img src="https://img.shields.io/github/stars/azure/azure-event-hubs-for-kafka.svg?style=social&label=Stars"
            alt="star our repo"></a>
  <a href="https://twitter.com/intent/follow?screen_name=azureeventhubs" target="_blank">
        <img src="https://img.shields.io/twitter/url/http/shields.io.svg?style=social&label=Follow%20@azureeventhubs"
            alt="follow on Twitter"></a>
</p></h1>

# Migrating to Azure Event Hubs for Apache Kafka Ecosystems

An Azure Event Hubs Kafka endpoint enables users to connect to Azure Event Hubs using the Kafka protocol. By making minimal changes to a Kafka application, users will be able to connect to Azure Event Hubs and reap the benefits of the Azure ecosystem. Event Hubs for Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

When we built Kafka-enabled Event Hubs, we wanted to give Kafka users the stability, scalability, and support of Event Hubs without sacrificing their ability to connect to the network of Kafka supporting frameworks. With that in mind, we've started rolling out a set of tutorials to show how simple it is to connect Kafka-enabled Event Hubs with various platforms and frameworks. The tutorials in this directory all work right out of the box, but for those of you looking to connect with a framework we haven't covered, this guide will outline the generic steps needed to connect your preexisting Kafka application to an Event Hubs Kafka endpoint.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For these samples, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Update your Kafka client configuration

To connect to a Kafka-enabled Event Hub, you'll need to update the Kafka client configs. If you're having trouble finding yours, try searching for where `bootstrap.servers` is set in your application.

Insert the following configs wherever makes sense in your application. Make sure to update the `bootstrap.servers` and `sasl.jaas.config` values to direct the client to your Event Hubs Kafka endpoint with the correct authentication. 

```
bootstrap.servers=mynamespace.servicebus.windows.net:9093
request.timeout.ms=60000
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="{YOUR.EVENTHUBS.CONNECTION.STRING}";
``` 

If `sasl.jaas.config` is not a supported configuration in your framework, find the configurations that are used to set the SASL username and password and use those instead. Set the username to `$ConnectionString` and the password to your Event Hubs connection string.

## Run your application

Run your application and see how it goes - in most cases this should be enough to make the switch. 

## Troubleshooting

### Receiving an UnknownServerException from Kafka client libraries

The error will look something like this:
```
java.util.concurrent.ExecutionException: org.apache.kafka.common.errors.UnknownServerException: The server experienced an unexpected error when processing the request
```
This error could mean many things, usually related to either client configuration or the configuration of the Event Hubs. Some cases where this has been seen are:

* Too many Kafka producers being started at once. Space out the Kafka producer startup
* Your requests are being throttled. One reason for this is too many producers sending events to too few partitions. Try creating a topic with more partitions.

### Consumers not getting any records and constantly rebalancing

There is no exception or error when this happens, but the Kafka logs will show that the consumers are stuck trying to re-join the group and assign partitions. If this is happening, ensure that all consumers are using unique client IDs by setting the `client.id` property for each consumer client. 

### Compression / Message Format Version issue

Kafka supports compression, and Event Hubs for Kafka currently does not. Errors that mention a message format version (e.g. `The message format version on the broker does not support the request.`) are usually caused when a client tries to send compressed Kafka messages to our brokers. 

If compressed data is necessary, compressing your data before sending it to the brokers and decompressing after receiving it is a valid workaround. The message body is just a byte array to the service, so client-side compression/decompression will not cause any issues.

### Other issues? 
In our experience, when changing the configurations didn't go as smoothly as we'd hoped, the issue was usually related to one of the following:

1. **Firewall issues** - Make sure that port 9093 isn't blocked by your firewall.

2. **TopicAuthorizationException** - The most common causes of this exception are:
    1. A typo in the connection string in your configuration file or
    2. Trying to use Event Hubs for Kafka on a Basic tier namespace. Event Hubs for Kafka is [only supported for Standard and Dedicated tier namespaces](https://azure.microsoft.com/pricing/details/event-hubs/).

3. **SASL authentication** - Getting your framework to cooperate with the SASL authentication protocol required by Event Hubs can be more difficult than meets the eye. See if you can troubleshoot the configuration using your framework's resources on SASL authentication. If you figure it out, let us know and we'll share it with other developers!

4. **Kafka version mismatch** - Event Hubs for Kafka Ecosystems supports Kafka versions 1.0 and later. Some applications using Kafka version 0.10 and later could occasionally work because of the Kafka protocol's backwards compatability, but there's a chance it won't be able to connect or will require some *serious* tinkering. Since Kafka versions 0.9 and earlier don't support the required SASL protocols, any adapter or client using those versions won't be able to connect to Event Hubs.

If you're still stuck (or if you know the secret to making it work with your framework), let us know by opening up a GitHub issue on this repo!

## List of differences between Event Hubs for Kafka Ecosystems and Apache Kafka

For the most part, the Event Hubs for Kafka Ecosystems has the same defaults, properties, error codes, and general behavior that Apache Kafka does. The instances where the two explicitly differ (or where Event Hubs imposes a limit that Kafka does not) are listed below:

* The max length of the `group.id` property is 256 characters
* The max size of `offset.metadata.max.bytes` is 1024 bytes
* Offset commits are throttled at 4 calls/second per partition with a max internal log size of 1Mb
