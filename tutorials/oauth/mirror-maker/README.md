# Using Apache Kafka's MirrorMaker with Event Hubs for Apache Kafka Ecosystems with OAuth

In this tutorial, we show how a Kafka-enabled Event Hub and Kafka MirrorMaker can integrate an existing Kafka pipeline into Azure by "mirroring" the Kafka input stream in the Event Hub service authorized via OAuth 2.0 flow. 

## Prerequisites

To complete this tutorial, make sure you have:

* An Azure subscription. If you do not have one, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.
* [Java Development Kit (JDK) 1.7+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
    * On Ubuntu, run `apt-get install default-jdk` to install the JDK.
    * Be sure to set the JAVA_HOME environment variable to point to the folder where the JDK is installed.
* [Download](http://maven.apache.org/download.cgi) and [install](http://maven.apache.org/install.html) a Maven binary archive
    * On Ubuntu, you can run `apt-get install maven` to install Maven.
* [Git](https://www.git-scm.com/downloads)
    * On Ubuntu, you can run `sudo apt-get install git` to install Git.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create a Kafka-enabled Event Hub](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For these samples, you will need the Fully Qualified Domain Name of you Event Hubs namespace which can be found in Azure Portal. To do so, in Azure Portal, go to your Event Hubs namespace overview page and copy host name which should look like `mynamespace.servicebus.windows.net`.

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Create an Azure Active Directory Application

In order to run these samples, you will need to create an AAD application with a client secret and assign it as EventHubs Data Owner on the Event Hubs namespace you created in the previous section.

Learn more about [AAD Role Based Access Control](https://docs.microsoft.com/en-us/azure/role-based-access-control/overview)

Learn more about [Azure EventHubs Role Based Access Control](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory)

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and navigate to the `tutorials/oauth/mirror-maker` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/oauth/mirror-maker
```

## Set up a Kafka cluster

Use the [Kafka quickstart guide](https://kafka.apache.org/quickstart) to set up a cluster with the desired settings (or use an existing Kafka cluster).

## Kafka MirrorMaker

Kafka MirrorMaker allows for the "mirroring" of a stream. Given source and destination Kafka clusters, MirrorMaker will ensure any messages sent to the source cluster will be received by both the source *and* destination clusters. In this example, we'll show how to mirror a source Kafka cluster with a destination Kafka-enabled Event Hub. This scenario can be used to send data from an existing Kafka pipeline to Event Hubs without interrupting the flow of data. 

Check out the [Kafka Mirroring/MirrorMaker Guide](https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=27846330) for more detailed information on Kafka MirrorMaker.

### Configuration

To configure Kafka MirrorMaker, we'll give it a Kafka cluster as its consumer/source and a Kafka-enabled Event Hub as its producer/destination.

#### Consumer Configuration

Update the consumer configuration file `source-kafka.config`, which tells MirrorMaker the properties of the source Kafka cluster.

##### source-kafka.config

```config
bootstrap.servers={SOURCE.KAFKA.IP.ADDRESS1}:{SOURCE.KAFKA.PORT1},{SOURCE.KAFKA.IP.ADDRESS2}:{SOURCE.KAFKA.PORT2},etc
group.id=example-mirrormaker-group
exclude.internal.topics=true
client.id=mirror_maker_consumer
```

#### Producer Configuration

Now update the producer config file `mirror-eventhub.config`, which tells MirrorMaker to send the duplicated (or "mirrored") data to the Event Hubs service. Specifically change `bootstrap.servers` and `sasl.jaas.config` to point to your Event Hubs Kafka endpoint. The Event Hubs service requires secure (SASL) communication, which is achieved by setting the last three properties in the configuration below. 

##### mirror-eventhub.config

```config
bootstrap.servers=mynamespace.servicebus.windows.net:9093
client.id=mirror_maker_producer

#Required for Event Hubs
sasl.mechanism=PLAIN
security.protocol=SASL_SSL
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX";
```

### Run MirrorMaker

Run the Kafka MirrorMaker script from the root Kafka directory using the newly updated configuration files. Make sure to update the path of the config files (or copy them to the root Kafka directory) in the following command.

```bash
bin/kafka-mirror-maker.sh --consumer.config source-kafka.config --num.streams 1 --producer.config mirror-eventhub.config --whitelist=".*"
```

To verify that events are making it to the Kafka-enabled Event Hub, check out the ingress statistics in the [Azure portal](https://azure.microsoft.com/features/azure-portal/), or run a consumer against the Event Hub.

Now that MirrorMaker is running, any events sent to the source Kafka cluster should be received by both the Kafka cluster *and* the mirrored Kafka-enabled Event Hub service. By using MirrorMaker and an Event Hubs Kafka endpoint, we can migrate an existing Kafka pipeline to the managed Azure Event Hubs service without changing the existing cluster or interrupting any ongoing data flow!
