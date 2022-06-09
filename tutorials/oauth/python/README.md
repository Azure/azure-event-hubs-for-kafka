# Send Messages in Python using Azure Event Hubs for Apache Kafka Ecosystem with OAuthBearer

This quickstart will show how to create and connect to an Event Hubs Kafka endpoint using an example producer written in Python. See the [kafka-python docs](https://kafka-python.readthedocs.io/en/master/index.html) for more information on how to use Kafka clients in Python.

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

Additionally, you need to install:

* [Python](https://www.python.org/downloads/)
* [Git](https://www.git-scm.com/downloads)
    * On Ubuntu, you can run `sudo apt-get install git` to install Git.
* [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli)
* [Install kafka-python library](https://github.com/dpkp/kafka-python)
    * Run `pip install kafka-python`.
* [Install azure-identity library](https://github.com/Azure/azure-sdk-for-python/tree/main/sdk/identity/azure-identity)
    * Run `pip install azure-identity`.

## Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint.

Additionally, topics in Kafka map to Event Hub instances, so create an Event Hub instance called "example_topic" that our samples can send and receive messages from.

### FQDN

For these samples, you will need the Fully Qualified Domain Name of your Event Hubs namespace which can be found in Azure Portal. To do so, in Azure Portal, go to your Event Hubs namespace overview page and copy host name which should look like `**`mynamespace.servicebus.windows.net`**`.

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. \*.servicebus.chinacloudapi.cn, \*.servicebus.usgovcloudapi.net, or \*.servicebus.cloudapi.de).

## Provision the correct permissions to yourself

In order to run these samples, you will need to assign yourself the role "Event Hubs Data Sender", scoped to the Event Hubs namespace you created in the previous section. 

Learn more about [AAD Role Based Access Control](https://docs.microsoft.com/en-us/azure/role-based-access-control/overview)

Learn more about [Azure Event Hubs Role Based Access Control](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory)

## ALTERNATIVE: Create an Azure Active Directory Application

An alternative for using your own personal credentials for authenticating with the Event Hub, you can create a service principal for the application from which you will send events to the Event Hub. You will need to create an AAD application with a client secret and assign it as Event Hubs Data Sender on the Event Hubs namespace you created in the previous section.

Learn more about [AAD Role Based Access Control](https://docs.microsoft.com/en-us/azure/role-based-access-control/overview)

Learn more about [Azure Event Hubs Role Based Access Control](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory)


## Clone the example project

Now that you have registered your Kafka-enabled Event Hubs namespace in the AAD, clone the Azure Event Hubs for Kafka repository and navigate to the `tutorials/oauth/python` subfolder:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/oauth/python
```

## Configuration

In the ./producer/config/oauth_config.json file, make sure you replace the NAMESPACE placeholders with the name of your actual Event Hubs namespace.

## Login to Azure CLI

The Python producer in this example makes use of the credentials with which you are authenticated in the Azure CLI. To login to the Azure CLI, run `az login` in your terminal. In the browser pop-up, login to the Azure account for which you have provisioned the 'Event Hub Data sender' permission. 

Alternatively, if you created a service principal with 'Event Hub Data sender' permissions, run `az login --service-principal -u <app-id> -p <password> --tenant <tenant>` to use the service principal's credentials on your local machine, with `app_id` being the application id of your registered application, and `password` being a client secret you have created for your registered application. 

If you want to run the producer script on a machine which already has a service principal account which it is configured to use for communication with other Azure resources, perform the following steps:
1. Go to the ./producer/src/token_provider.py file
2. In line 14, change `AzureCliCredential()` to `DefaultAzureCredential()`.


## Producer

The producer sample demonstrates how to send messages to the Event Hubs service using the Kafka protocol.

You can run the sample via:

```bash
$ cd producer/src
$ python producer.py
```

The producer will now begin sending events to the Kafka-enabled Event Hub on topic `example_topic` and printing the events to your console. If you would like to change the topic, change the topic variable in `producer.py`.

The producer can be stopped by using CTRL+C in the terminal.
