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

## Creating an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For these samples, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Updating your Kafka client configuration

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

## Troubleshooting

### Kafka Throttling

With Event Hubs AMQP clients, a ServerBusy exception is immediately returned upon service throttling, equivalent to a “try again later” message.  In Kafka, messages are just delayed before being completed, and the delay length is returned in milliseconds as `throttle_time_ms` in the produce/fetch response. In most cases, these delayed requests are not logged as ServerBusy exceptions on Event Hubs dashboards – instead, the response's `throttle_time_ms` value should be used as an indicator that throughput has exceeded the provisioned quota.

If traffic is extremely excessive, the service has the following behavior:
* If produce request’s delay exceeds request timeout – EH returns PolicyViolation error code
* If fetch request’s delay exceeds request timeout – EH logs the request as throttled and responds with empty set of records and no error code

Dedicated clusters do not have throttling mechanisms - you are free to consume all of your cluster resources.  An overview on dedicated clusters can be found [here](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-dedicated-overview).

### Consumers not getting any records and constantly rebalancing

There is no exception or error when this happens, but the Kafka logs will show that the consumers are stuck trying to re-join the group and assign partitions. There are a few possible causes:

 * Make sure that your `request.timeout.ms` is at least the recommended value of 60000 and your `session.timeout.ms` is at least the recommended value of 30000. Having these too low could cause consumer timeouts which then cause rebalances (which then cause more timeouts which then cause more rebalancing...) 
 * If your configuration matches those recommended values, and you're still seeing constant rebalancing, feel free to open up an issue (make sure to include your entire configuration in the issue so we can help debug)!

### Compression / Message Format Version issue

Kafka supports compression, and Event Hubs for Kafka currently does not. Errors that mention a message format version (e.g. `The message format version on the broker does not support the request.`) are usually caused when a client tries to send compressed Kafka messages to our brokers. 

If compressed data is necessary, compressing your data before sending it to the brokers and decompressing after receiving it is a valid workaround. The message body is just a byte array to the service, so client-side compression/decompression will not cause any issues.

### Receiving an UnknownServerException from Kafka client libraries

**Note: if you are using `rdkafka`-based libraries and you are seeing issues where producers get 100% timeout failures, please upgrade your `rdkafka` installation to the latest version (> v1.5.2).**

The error will look something like this:
```
org.apache.kafka.common.errors.UnknownServerException: The server experienced an unexpected error when processing the request
```
Please open an issue.  Debug-level logging and exception timestamps in UTC are extremely helpful. 


### Other issues?
Check the following items if experiencing issues when using Kafka on Event Hubs.

1. **Firewall blocking traffic** - Make sure that port 9093 isn't blocked by your firewall.

2. **TopicAuthorizationException** - The most common causes of this exception are:
    1. A typo in the connection string in your configuration file, or
    2. Trying to use Event Hubs for Kafka on a Basic tier namespace. Event Hubs for Kafka is [only supported for Standard and Dedicated tier namespaces](https://azure.microsoft.com/pricing/details/event-hubs/).

3. **Kafka version mismatch** - Event Hubs for Kafka Ecosystems supports Kafka versions 1.0 and later. Some applications using Kafka version 0.10 and later could occasionally work because of the Kafka protocol's backwards compatability, but we heavily recommend against using old API versions. Kafka versions 0.9 and earlier do not support the required SASL protocols and will not be able to connect to Event Hubs.

4. **Strange encodings on AMQP headers when consuming with Kafka** - when sending to Event Hubs over AMQP, any AMQP payload headers are serialized in AMQP encoding.  Kafka consumers will not deserialize the headers from AMQP - to read header values, you must manually decode the AMQP headers.  (Alternatively, you can avoid using AMQP headers if you know that you will be consuming via Kafka protocol.) See here - https://github.com/Azure/azure-event-hubs-for-kafka/issues/56

5. **SASL authentication** - Getting your framework to cooperate with the SASL authentication protocol required by Event Hubs can be more difficult than meets the eye. See if you can troubleshoot the configuration using your framework's resources on SASL authentication. If you figure it out, let us know and we'll share it with other developers!

If you're still stuck (or if you know the secret to making it work with your framework), let us know by opening up a GitHub issue on this repo!

## Apache Kafka vs. Event Hubs Kafka

For the most part, the Event Hubs for Kafka Ecosystems has the same defaults, properties, error codes, and general behavior that Apache Kafka does. The instances where the two explicitly differ (or where Event Hubs imposes a limit that Kafka does not) are listed below:

* The max length of the `group.id` property is 256 characters
* The max size of `offset.metadata.max.bytes` is 1024 bytes

## More FAQ

**Are you running Apache Kafka?**

No.  We execute Kafka API operations against Event Hubs infrastructure.  Because there is a tight correlation between Apache Kafka and Event Hubs AMQP functionality (i.e. produce, receive, management, etc.), we are able to bring the known reliability of Event Hubs to the Kafka PaaS space.

**What's the difference between an Event Hub consumer group and a Kafka consumer group on Event Hubs?**

Kafka consumer groups on EH are fully distinct from standard Event Hubs consumer groups.

Event Hubs consumer groups are...
- managed with CRUD operations via portal, SDK, or ARM templates.  EH consumer groups cannot be auto-created.
- children entities of an Event Hub.  This means that the same consumer group name can be reused between Event Hubs in the same namespace because they are separate entities.
- not used for storing offsets.  Orchestrated AMQP consumption is done using external offset storage, e.g. [Event Processor Host](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-event-processor-host) and an offset store like Azure Storage.

Kafka consumer groups are...
- auto-created.  Kafka groups can be managed via the Kafka consumer group APIs.
- capable of storing offsets in the Event Hubs service.
- used as keys in what is effectively an offset key-value store.  For a unique pair of group.id and topic-partition, we store an offset in Azure Storage (3x replication).  Event Hubs users will not incur extra storage costs from storing Kafka offsets.  Offsets are manipulable via the Kafka consumer group APIs, but the offset storage *accounts* are not directly visible or manipulable for Event Hub users.  
- span a namespace.  Using the same Kafka group name for multiple applications on multiple EH topics means that all applications and their Kafka clients will be rebalanced whenever only a single application needs rebalancing.  Choose your group names wisely.
- fully distinct from EH consumer groups.  You **do not** need to use '$Default', nor do you need to worry about Kafka clients interfering with AMQP workloads.
- not viewable in the Azure portal.  Consumer group info is accessible via Kafka APIs.
