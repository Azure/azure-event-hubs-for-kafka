# Send and Receive Messages in Java using Azure Event Hubs for Apache Kafka Ecosystems with OAuth

This tutorial will show how to create and connect to an Event Hubs Kafka endpoint using an example producer and consumer written in Java. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

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

For these samples, you will need the Fully Qualified Domain Name of you Event Hubs namespace which can be found in Azure Portal. To do so, in Azure Portal, go to your Event Hubs namespace overview page and copy host name which should look like `**`<your-namespace>.servicebus.windows.net`**`.

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Create an Azure Active Directory Application

In order to run these samples, you will need to create an AAD application with a client secret and assign it as EventHubs Data Owner on the Event Hubs namespace you created in the previous section.

Learn more about [AAD Role Based Access Control](https://docs.microsoft.com/en-us/azure/role-based-access-control/overview)

Learn more about [Azure EventHubs Role Based Access Control](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory)

## Clone the example project

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and navigate to the `quickstart/java` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/oauth/java/appsecret
```

## Client Configuration For OAuth
Kafka clients need to be configured in a way that they can authenticate with Azure Active Directory and fetch OAuth access tokens. These tokens then can be used to get authorized while accessing certain Event Hubs resources.

#### Here is a list of authentication parameters for Kafka clients that needs to be configured for all clients

* Set SASL mecnahism to OAUTHBEARER

   `sasl.mechanism=OAUTHBEARER`
* Set Java Authentication and Authorization Service (JAAS) configuration to OAuthBearerLoginModule

   `sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;`
* Set login callback handler. This is the authentication handler which is responsible to complete oauth flow and return an access token.

   `sasl.login.callback.handler.class=CustomAuthenticateCallbackHandler`

#### Configure authenticate callback handler of your client so that it can complete auth flow with Azure Active Directory and fetch access tokens

* Set authority for your tenant. Most of the times, this is a URI built with your AAD tenant identifier such as  `"https://login.microsoftonline.com/<tenant-id>/"`

   `this.authority = "https://login.microsoftonline.com/<tenant-id>/";`

* Set your AAD application identifier, also known as client id

   `this.appId = "<app-id>";`

 * Set your AAD application password, also known as client secret

   `this.appSecret = "<app-password>";`

## Producer

Using the provided producer example, send messages to the Event Hubs service. To change the Kafka version, change the dependency in the pom file to the desired version.

### Provide an Event Hubs Kafka endpoint

#### producer.config

Update the `bootstrap.servers` in `producer/src/main/resources/producer.config` to direct the producer to the Event Hubs Kafka endpoint.

```config
bootstrap.servers=mynamespace.servicebus.windows.net:9093 # REPLACE
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
sasl.login.callback.handler.class=CustomAuthenticateCallbackHandler
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

Change the `bootstrap.servers` in `consumer/src/main/resources/consumer.config` to direct the consumer to the Event Hubs endpoint.

```config
bootstrap.servers=mynamespace.servicebus.windows.net:9093 # REPLACE
group.id=$Default
request.timeout.ms=60000
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
sasl.jaas.config=org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;
sasl.login.callback.handler.class=CustomAuthenticateCallbackHandler
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
