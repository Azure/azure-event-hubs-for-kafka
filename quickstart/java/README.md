# Send and Receive Messages in Java using Azure Event Hubs for Apache Kafka Ecosystems

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using an example producer and consumer written in Java. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

In addition:

* [Java Development Kit (JDK) 1.7+](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
    * On Ubuntu, run `apt-get install default-jdk` to install the JDK.
    * Be sure to set the JAVA_HOME environment variable to point to the folder where the JDK is installed.
* [Download](http://maven.apache.org/download.cgi) and [install](http://maven.apache.org/install.html) a Maven binary archive
    * On Ubuntu, you can run `apt-get install maven` to install Maven.
* [Git](https://www.git-scm.com/downloads)
    * On Ubuntu, you can run `sudo apt-get install git` to install Git.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For these samples, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

`Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX`

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and navigate to the `quickstart/java` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/quickstart/java
```

## Producer

Using the provided producer example, send messages to the Event Hubs service. To change the Kafka version, change the dependency in the pom file to the desired version.

### Provide an Event Hubs Kafka endpoint

#### producer.config

Update the `bootstrap.servers` and `sasl.jaas.config` values in `producer/src/main/resources/producer.config` to direct the producer to the Event Hubs Kafka endpoint with the correct authentication.

```config
bootstrap.servers=mynamespace.servicebus.windows.net:9093
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX";
```

### Run producer from command line

This sample is configured to send messages to topic `test`, if you would like to change the topic, change the TOPIC constant in `producer/src/main/java/TestProducer.java`.

To run the producer from the command line, generate the JAR and then run from within Maven (alternatively, generate the JAR using Maven, then run in Java by adding the necessary Kafka JAR(s) to the classpath):

```bash
mvn clean package
mvn exec:java -Dexec.mainClass="TestProducer"
```

The producer will now begin sending events to the Kafka-enabled Event Hub at topic `test` (or whatever topic you chose) and printing the events to stdout. 

## Consumer

Using the provided consumer example, receive messages from the Kafka-enabled Event Hubs. To change the Kafka version, change the dependency in the pom file to the desired version.

### Provide an Event Hubs Kafka endpoint

#### consumer.config

Change the `bootstrap.servers` and `sasl.jaas.config` values in `consumer/src/main/resources/consumer.config` to direct the consumer to the Event Hubs endpoint with the correct authentication.

```config
bootstrap.servers=mynamespace.servicebus.windows.net:9093
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX";
```

### Run consumer from command line

This sample is configured to receive messages from topic `test`, if you would like to change the topic, change the TOPIC constant in `consumer/src/main/java/TestConsumer.java`.

To run the producer from the command line, generate the JAR and then run from within Maven (alternatively, generate the JAR using Maven, then run in Java by adding the necessary Kafka JAR(s) to the classpath):

```bash
mvn clean package
mvn exec:java -Dexec.mainClass="TestConsumer"
```

If the Kafka-enabled Event Hub has incoming events (for instance, if your example producer is also running), then the consumer should now begin receiving events from topic `test` (or whatever topic you chose).

By default, Kafka consumers will read from the end of the stream rather than the beginning. This means any events queued before you begin running your consumer will not be read. If you started your consumer but it isn't receiving any events, try running your producer again while your consumer is polling. Alternatively, you can use Kafka's [`auto.offset.reset` consumer config](https://kafka.apache.org/documentation/#newconsumerconfigs) to make your consumer read from the beginning of the stream!
