# Using Confluent's Python Kafka client and librdkafka with Event Hubs for Apache Kafka Ecosystems

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using an example producer and consumer written in python. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

This sample is based on [Confluent's Apache Kafka Python client](https://github.com/confluentinc/confluent-kafka-python), modified for use with Event Hubs for Kafka.  While the tutorial is aimed at Linux users, MacOS users can follow along with Homebrew.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

In addition:

* [Git](https://www.git-scm.com/downloads)
* [Python](https://www.python.org/downloads/) (versions 2.7.x and 3.6.x are fine)
* [Pip](https://pypi.org/project/pip/)
* [OpenSSL](https://www.openssl.org/) (including libssl)
* [librdkafka](https://github.com/edenhill/librdkafka)

Running the setup script provided in this repo will install and configure all of the required dependencies.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

## Getting ready

Now that you have a Kafka-enabled Event Hubs connection string, clone the Azure Event Hubs for Kafka repository and navigate to the `quickstart/python` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/quickstart/python
```

Now run the set up script:

```shell
source setup.sh
```

(If using MacOS, you can use Homebrew to set up your machine by running `brew install openssl python librdkafka` - `pip` can then be used to install the Python SDK with `pip install confluent-kafka`.)

## Running the samples

### Update your configurations

Both the producer and consumer samples require extra configuration to authenticate with your Event Hubs namespace. Add required configurations to environment (.env) file at the root folder.

    AZURE_AUTHORITY_HOST=login.microsoftonline.com

    AZURE_CLIENT_ID=<<AppClientId>>

    AZURE_CLIENT_SECRET=<<AppSecret>>

    AZURE_TENANT_ID=<<TenantID>>

    AZURE_CLIENT_CERTIFICATE_PATH=<<AzureAdClientSecretCertPath>> ## For Certs remove the AZURE_CLIENT_SECRET entry from .env file
    
    AZURE_CLIENT_SEND_CERTIFICATE_CHAIN=<<TrueToValidateISsuesAndSubjectName>>
  
    EVENT_HUB_HOSTNAME=<<EventHubNameSpace>>

    EVENT_HUB_NAME=<<EvemtHubName>>

    CONSUMER_GROUP=<<ConsumerGroupName>>

### Producing
 
```shell 
python producer.py <topic>
```

Note that the topic must already exist or else you will see an "Unknown topic or partition" error.

### Consuming

```shell
python consumer.py 
```

### Dependencies

    **Azure.Identity** -> For Azure AD AUTH. Please refer defaultazurecredential

    **Confluent-Kafka** -> To connect to EventHub using Kafka protocol

