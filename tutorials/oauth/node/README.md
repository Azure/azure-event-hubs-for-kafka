# Send and Receive Messages in Javascript (NodeJS) using Azure Event Hubs for Apache Kafka Ecosystems with OAuth support and KafkaJS

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using an example producer and consumer written in Javascript for NodeJS using [KafkaJS](https://kafka.js.org/). Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

In addition:
* [NodeJS](https://nodejs.org/)
    * For Ubuntu, check the following article on installing NodeJS https://tecadmin.net/install-latest-nodejs-npm-on-ubuntu/#:~:text=How%20to%20Install%20Latest%20Node.js%20on%20Ubuntu%20with,Server%20%28Optional%29%20This%20is%20an%20optional%20step.%20
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

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and navigate to the `tutorials/oauth/node` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/oauth/node
```

## Client Configuration For OAuth
Kafka clients need to be configured in a way that they can authenticate with Azure Active Directory and fetch OAuth access tokens. These tokens then can be used to get authorized while accessing certain Event Hubs resources.

#### Here is a list of authentication parameters for Kafka clients that needs to be configured for all clients

* Set SASL mecnahism to OAUTHBEARER

More details at: https://kafka.js.org/docs/1.13.0/configuration#oauthbearer-example

#### Configure authenticate callback handler of your client so that it can complete auth flow with Azure Active Directory and fetch access tokens

* Set authority for your tenant. Most of the times, this is a URI built with your AAD tenant identifier such as `"https://login.microsoftonline.com/<tenant-id>/"`

  `const tenantId = '<tenant-id';`
  `const authorityHostUrl = 'https://login.microsoftonline.com';`
  `const authorityUrl = `${authorityHostUrl}/${tenantId}/`;`

* Set your AAD application identifier, also known as client id

   `const appId = '<app-id>';`

 * Set your AAD application password, also known as client secret

   `this.appPassword = "<app-password>";`

## Producer

Using the provided producer example, send messages to the Event Hubs service. To change the KafkaJS version, change the dependency in the package.json file to the desired version.

### Run producer from command line

This sample is configured to send messages to topic `test`, if you would like to change the topic, change the TOPIC constant in `producer.js`.

To run the producer from the command line:

```bash
node producer.js
```

The producer will now begin sending events to the Kafka-enabled Event Hub at topic `test` (or whatever topic you chose) and printing the status to stdout.

## Consumer

Using the provided consumer example, receive messages from the Kafka-enabled Event Hubs.
To change the Kafka version, change the dependency in the package.json file to the desired version.

### Run consumer from command line

This sample is configured to receive messages from topic `test`, if you would like to change the topic, change the TOPIC constant in `consumer.js`.

To run the consumer from the command line:

```bash
node consumer.js
```

If the Kafka-enabled Event Hub has incoming events (for instance, if your example producer has already ran), then the consumer should now begin receiving events from topic `test` (or whatever topic you chose).

In this example, by default, Kafka consumers will read from the begining of the stream rather than from the end. This means any events queued before you begin running your consumer will be read. If you started your consumer but it isn't receiving any events, try running your producer again while your consumer is polling just in case. Alternatively, you can use KafkaJS feature [`await consumer.subscribe({ topic: TOPIC, fromBeginning: false });` from beginning to false](https://kafka.js.org/docs/1.11.0/consuming#from-beginning) to make your consumer NOT to read from the beginning of the stream!
