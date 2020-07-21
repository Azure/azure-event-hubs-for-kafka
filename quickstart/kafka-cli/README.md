# Use the Kafka CLI to Send and Receive Messages to/from Azure Event Hubs for Apache Kafka Ecosystem

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using the CLI which comes bundled with the Apache Kafka distribution. 

> Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https:/kafka.apache.org/10/documentation.html) and later.

## Prerequisites

- If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/) before you begin.
- You will need to install the [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli?view=azure-cli-latest) if you don't have it already.
- You will also need Apache Kafka, which you can [download from the official website](https://kafka.apache.org/downloads).

Clone the Azure Event Hubs for Kafka repository and navigate to `quickstart/kafka-cli` subfolder:

    git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
    cd azure-event-hubs-for-kafka/quickstart/kafka-cli

## Create your Kafka enabled Event Hubs cluster

Set the required variables

    export AZURE_SUBSCRIPTION=[to be filled]
    export AZURE_RESOURCE_GROUP=[to be filled]
    export AZURE_LOCATION=[to be filled]
    export EVENT_HUBS_NAMESPACE=[to be filled]
    export EVENT_HUB_NAME=[to be filled]

Create an Azure resource group if you don't have one already

    az account set --subscription $AZURE_SUBSCRIPTION
    az group create --name $AZURE_RESOURCE_GROUP --location $AZURE_LOCATION

Create an [Event Hubs namespace](https://docs.microsoft.com/azure/event-hubs/event-hubs-features) (similar to a Kafka Cluster)

    az eventhubs namespace create --name $EVENT_HUBS_NAMESPACE --resource-group $AZURE_RESOURCE_GROUP --location $AZURE_LOCATION --enable-kafka true --enable-auto-inflate false

And then create an Event Hub (same as a Kafka topic)

    az eventhubs eventhub create --name $EVENT_HUB_NAME --resource-group $AZURE_RESOURCE_GROUP --namespace-name $EVENT_HUBS_NAMESPACE --partition-count 2

## Event Hubs connection details

Get the connection string and credentials for your cluster

> For details, read [how Event Hubs uses Shared Access Signatures for authorization](https://docs.microsoft.com/azure/event-hubs/authorize-access-shared-access-signature?WT.mc_id=devto-blog-abhishgu)

Start by fetching the Event Hub rule/policy name

    az eventhubs namespace authorization-rule list --resource-group $AZURE_RESOURCE_GROUP --namespace-name $EVENT_HUBS_NAMESPACE

You will get a JSON output similar to below:

    [
        {
            "id": "/subscriptions/qwerty42-ae29-4924-b6a7-dda0ea91d347/resourceGroups/foobar-resource/providers/Microsoft.EventHub/namespaces/foobar-event-hub-ns/AuthorizationRules/RootManageSharedAccessKey",
            "location": "Southeast Asia",
            "name": "RootManageSharedAccessKey",
            "resourceGroup": "foobar-resource",
            "rights": [
            "Listen",
            "Manage",
            "Send"
            ],
            "type": "Microsoft.EventHub/Namespaces/AuthorizationRules"
        }
    ]

The authroization rule name is the value of the `name` attribute (without the quotes), which in this case is `RootManageSharedAccessKey`

And, then make use of the rule name to extract the connection string

    az eventhubs namespace authorization-rule keys list --resource-group $AZURE_RESOURCE_GROUP --namespace-name $EVENT_HUBS_NAMESPACE --name $EVENT_HUB_AUTH_RULE_NAME

You'll get a JSON response as such:

    {
        "aliasPrimaryConnectionString": null,
        "aliasSecondaryConnectionString": null,
        "keyName": "RootManageSharedAccessKey",
        "primaryConnectionString": "Endpoint=sb://foobar-eventhub-namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Nbaz0D42MT7qwerty6D/W51ao42r6EJuxR/zEqwerty=",
        "primaryKey": "qwertyEiQHIirSNDPzqcqvZEUs6VAW+JIK3L46tqwerty",
        "secondaryConnectionString": "Endpoint=sb://abhishgu-temp-event-hub-ns.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=qwertyPF2/YRGzxKmb06Z8NBFLCjnX38O7ch6aiYkN0=",
        "secondaryKey": "qwertyPF2/YRGzxKmb06Z8NBqwertyX38O7ch6aiYk42="
    }

The primary connection string is the value of the `primaryConnectionString` attribute (without the quotes), which in this case is `Endpoint=sb://foobar-eventhub-namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Nbaz0D42MT7qwerty6D/W51ao42r6EJuxR/zEqwerty=`

> [This information is sensitive - please excercise caution](https://docs.microsoft.com/azure/event-hubs/authorize-access-shared-access-signature?#best-practices-when-using-sas)

Update the `password` field in the `jaas.conf` with the primary connection string

> **Please do not remove** the trailing `;` in the `password` field of the `jaas.conf` file

    KafkaClient {
        org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="enter-connection-string-here";
    };

For example:

    KafkaClient {
        org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="Endpoint=sb://foobar-eventhub-namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Nbaz0D42MT7qwerty6D/W51ao42r6EJuxR/zEqwerty=";
    };

## Connect to Event Hubs using Kafka CLI

On your local machine, use a new terminal to start a **Kafka Consumer** - set the required variables first

    export EVENT_HUBS_NAMESPACE=[to be filled]
    export EVENT_HUB_NAME=[to be filled]
    export export KAFKA_OPTS="-Djava.security.auth.login.config=jaas.conf"
    export KAFKA_INSTALL_HOME=[to be filled] e.g. /Users/foo/kafka_2.12-2.3.0/

Start consuming

    $KAFKA_INSTALL_HOME/bin/kafka-console-consumer.sh --topic $EVENT_HUB_NAME --bootstrap-server $EVENT_HUBS_NAMESPACE.servicebus.windows.net:9093 --consumer.config client_common.properties

e.g.

    /Users/foo/kafka_2.12-2.3.0/bin/kafka-console-consumer.sh --topic foobar-topic --bootstrap-server foobar-eventhub-ns.servicebus.windows.net:9093 --consumer.config client_common.properties

Use another terminal to start a **Kafka Producer** - set the required variables first

    export EVENT_HUBS_NAMESPACE=[to be filled]
    export EVENT_HUB_NAME=[to be filled]
    export export KAFKA_OPTS="-Djava.security.auth.login.config=jaas.conf"
    export KAFKA_INSTALL_HOME=[to be filled] e.g. /Users/foo/kafka_2.12-2.3.0/

Start the producer

    $KAFKA_INSTALL_HOME/bin/kafka-console-producer.sh --topic $EVENT_HUB_NAME --broker-list $EVENT_HUBS_NAMESPACE.servicebus.windows.net:9093 --producer.config client_common.properties

e.g.

    /Users/foo/kafka_2.12-2.3.0/bin/kafka-console-producer.sh --topic foobar-topic --broker-list foobar-eventhub-ns.servicebus.windows.net:9093 --producer.config client_common.properties

You will get a prompt and you can start entering values:

    > foo
    > bar
    > baz
    > john
    > doe

Switch over to the consumer terminal to confirm you have got the messages.

## Clean up

Finally, if you want to delete the Azure Event Hubs resources:

    az group delete --name $AZURE_RESOURCE_GROUP --yes --no-wait

> Please note that this will delete all the resources under the resource group.