## Use Sarama Kafka Go client to send and receive message using Azure Event Hubs for Apache Kafka Ecosystem

This quickstart will show how to create and connect to an [Event Hubs Kafka](https://docs.microsoft.com/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview?WT.mc_id=azureeventhubsforkafka-github-abhishgu) endpoint using an example producer and consumer written in Go using the [Sarama Kafka client](https://github.com/Shopify/sarama) library

Note: Azure Event Hubs for Apache Kafka Ecosystems supports Apache Kafka version 1.0 and later.

## Prerequisites

- If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?WT.mc_id=azureeventhubsforkafka-github-abhishgu) before you begin.
- You will need to install the [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli?view=azure-cli-latest&WT.mc_id=azureeventhubsforkafka-github-abhishgu) if you don't have it already.

In addition, you will also need:

- [Go installed](https://golang.org/doc/install)
- [Git](https://www.git-scm.com/downloads)
    * On Ubuntu, you can run `sudo apt-get install git` to install Git.



## Create your Kafka enabled Event Hubs namespace

If you have a namespace already, skip this and go to the *"Event Hubs connection details"* section

Set some variables to avoid repetition

    export AZURE_SUBSCRIPTION=[to be filled]
    export AZURE_RESOURCE_GROUP=[to be filled]
    export AZURE_LOCATION=[to be filled]
    export EVENT_HUBS_NAMESPACE=[name of the event hub namespace - to be filled]
    export EVENT_HUB_NAME=[name of the event hub (topic) - to be filled]

Create the resource group if you don't have one already

    az account set --subscription $AZURE_SUBSCRIPTION
    az group create --name $AZURE_RESOURCE_GROUP --location $AZURE_LOCATION

Create an Event Hubs namespace

> For details on Event Hubs namespace, please refer to the [Event Hubs documentation](https://docs.microsoft.com/azure/event-hubs/event-hubs-features?WT.mc_id=azureeventhubsforkafka-github-abhishgu)

    az eventhubs namespace create --name $EVENT_HUBS_NAMESPACE --resource-group $AZURE_RESOURCE_GROUP --location $AZURE_LOCATION --enable-kafka true --enable-auto-inflate false

> Documentation for [`az eventhubs namespace create`](https://docs.microsoft.com/cli/azure/eventhubs/namespace?view=azure-cli-latest&WT.mc_id=azureeventhubsforkafka-github-abhishgu#az-eventhubs-namespace-create)

And then create an Event Hub (same as a Kafka topic)

    az eventhubs eventhub create --name $EVENT_HUB_NAME --resource-group $AZURE_RESOURCE_GROUP --namespace-name $EVENT_HUBS_NAMESPACE --partition-count 10

> Documentation for [`az eventhub create`](https://docs.microsoft.com/cli/azure/eventhubs/eventhub?view=azure-cli-latest&WT.mc_id=azureeventhubsforkafka-github-abhishgu#az-eventhubs-eventhub-create)


### Event Hubs connection details

Get the connection string and credentials for your namespace

> For details, read [how Event Hubs uses Shared Access Signatures for authorization](https://docs.microsoft.com/azure/event-hubs/authorize-access-shared-access-signature?WT.mc_id=devto-blog-abhishgu)

Start by fetching the Event Hub rule/policy name

    az eventhubs namespace authorization-rule list --resource-group $AZURE_RESOURCE_GROUP --namespace-name $EVENT_HUBS_NAMESPACE

> Documentation for [`az eventhubs namespace authorization-rule list`](https://docs.microsoft.com/cli/azure/eventhubs/namespace/authorization-rule?view=azure-cli-latest&WT.mc_id=azureeventhubsforkafka-github-abhishgu#az-eventhubs-namespace-authorization-rule-list)


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

    export EVENT_HUB_AUTH_RULE_NAME=RootManageSharedAccessKey
    
And, then make use of the rule name to extract the connection string

    az eventhubs namespace authorization-rule keys list --resource-group $AZURE_RESOURCE_GROUP --namespace-name $EVENT_HUBS_NAMESPACE --name $EVENT_HUB_AUTH_RULE_NAME

> Documentation for [`az eventhubs namespace authorization-rule keys list`](https://docs.microsoft.com/cli/azure/eventhubs/namespace/authorization-rule/keys?view=azure-cli-latest&WT.mc_id=azureeventhubsforkafka-github-abhishgu#az-eventhubs-namespace-authorization-rule-keys-list)


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

The primary connection string is the value of the `primaryConnectionString` attribute (without the quotes), which in this case is `"Endpoint=sb://foobar-eventhub-namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Nbaz0D42MT7qwerty6D/W51ao42r6EJuxR/zEqwerty="`. Make a note of the connection string as you will be using it in the next step.

> [This information is sensitive - please excercise caution](https://docs.microsoft.com/azure/event-hubs/authorize-access-shared-access-signature?&WT.mc_id=azureeventhubsforkafka-github-abhishgu#best-practices-when-using-sas)


## Test producer and consumer

Clone the Azure Event Hubs for Kafka repository and navigate to quickstart/go-sarama subfolder:

    git clone https://github.com/Azure/azure-event-hubs-for-kafka.git

    cd azure-event-hubs-for-kafka/quickstart/go-sarama
    
Fetch the Sarama Kafka client library

    go get github.com/Shopify/sarama


### Producer

Set environment variables

    export EVENTHUBS_CONNECTION_STRING=[value of primary connection string obtained in the previous step]
    export EVENT_HUBS_NAMESPACE=[event hub namespace]
    export EVENTHUBS_BROKER=$EVENT_HUBS_NAMESPACE.servicebus.windows.net:9093
    export EVENTHUBS_TOPIC=[name of the event hub (topic)]

> for `EVENTHUBS_CONNECTION_STRING` variable, please ensure that you *include* the double-quotes in the value received using the Azure CLI e.g. 

    export EVENTHUBS_CONNECTION_STRING="Endpoint=sb://foobar-eventhub-namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Nbaz0D42MT7qwerty6D/W51ao42r6EJuxR/zEqwerty="

Start the producer

    go run producer/sarama-producer.go


Once it starts, you should see the logs

    Event Hubs broker [foo-bar.servicebus.windows.net:9093]
    Event Hubs topic testhub
    Waiting for program to exit...
    sent message to partition 0 offset 1
    sent message to partition 7 offset 1
    sent message to partition 6 offset 1
    sent message to partition 8 offset 1
    sent message to partition 2 offset 1

> To stop, just press ctrl+c on your terminal

### Consumer

Start the consumer process in a different terminal. Set environment variables


    export EVENTHUBS_CONNECTION_STRING=[value of primary connection string obtained in the previous step]
    export EVENT_HUBS_NAMESPACE=[event hub namespace]
    export EVENTHUBS_BROKER=$EVENT_HUBS_NAMESPACE.servicebus.windows.net:9093
    export EVENTHUBS_TOPIC=[name of the event hub (topic) - to be filled]
    export EVENTHUBS_CONSUMER_GROUPID=[name of consumer group e.g. testgroup]


> for `EVENTHUBS_CONNECTION_STRING` variable, please ensure that you *include* the double-quotes in the value received using the Azure CLI e.g. 

    export EVENTHUBS_CONNECTION_STRING="Endpoint=sb://foobar-eventhub-namespace.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=Nbaz0D42MT7qwerty6D/W51ao42r6EJuxR/zEqwerty="

Start the consumer

    go run consumer/sarama-consumer.go

In the logs, you will see that the consumer group gets created and all the partitions (10 in this example) are allocated to it

    Event Hubs broker [foo-bar.servicebus.windows.net:9093]
    Sarama client consumer group ID abhishgu
    new consumer group created
    Event Hubs topic testhub
    Waiting for program to exit
    Partition allocation - map[testhub:[0 1 2 3 4 5 6 7 8 9]]
    Message topic:"testhub" partition:9 offset:45
    Message content value-2019-10-08 16:12:23.704802 +0530 IST m=+1.003667284
    Message topic:"testhub" partition:3 offset:32
    Message content value-2019-10-08 17:05:42.388301 +0530 IST m=+0.912420074

In a different terminal, start another instance of the consumer. This will trigger a rebalance of the partitions and you will see that few (5 in this case) will get allocated to this (new) consumer instance

    Event Hubs broker [foo-bar.servicebus.windows.net:9093]
    Sarama client consumer group ID abhishgu
    new consumer group created
    Event Hubs topic testhub
    Waiting for program to exit
    Partition allocation - map[testhub:[0 1 2 3 4]]

If you go back to the terminal for the first consumer instance, you will see that few partitions have been taken away as a result of the rebalancing

    Consumer group clean up initiated
    Partition allocation - map[testhub:[5 6 7 8 9]]

> To stop, just press ctrl+c on your terminal

Now, both the consumers will share the workload and consume messages from Event Hubs. You keep scaling out by starting more consumer instances, but this will only be useful till you reach the point where number of consumer instances is equal to the number of partitions. In essence, the number pf partitions of your Event Hub is the unit of parallelism and scale as far as consuming messages from Event Hub Kafka is concerned.
