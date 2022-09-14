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


## OAuth 2.0 Overview

Event Hubs integrates with Azure Active Directory (Azure AD), which provides an OAuth 2.0 compliant authorization server. Azure role-based access control (Azure RBAC) can be used to grant permissions to Kafka client identities. 

To grant access to an Event Hub resource, the security principal must be authenticated and an OAuth 2.0 token is returned. The token is then passed as part of the request to the Event Hub Service to authorize access to the resource. This is accomplished by setting the appropriate values in the Kafka client configuration. 

For more information on the Azure AD OAuth 2.0 for Event Hubs see [Authorize access with Azure Active Directory](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory#overview)

### Retrieve Azure Active Directory (AAD) Token

The `DefaultAzureCredential` Class can be used to get a credential, and Kafka clients must use the scope of `https://<namespace>.servicebus.windows.net` to retrieve the access token from the Event Hub namespace. See [DefaultAzureCredential](https://docs.microsoft.com/en-us/python/api/azure-identity/azure.identity.defaultazurecredential?view=azure-python) for more information on this class. 

In this example, a service principal is used to get the AAD credential by setting the following environment variables:    

```shell
export AZURE_TENANT_ID=<TenantID>
export AZURE_CLIENT_ID=<AppClientId>
export AZURE_CLIENT_SECRET=<AppSecret>
```

An RBAC role must be assigned to the application to gain access to Event Hubs. Azure provides built-in roles for authorizing access to Event Hubs. See [Azure Built in Roles for Azure Event Hubs](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory#azure-built-in-roles-for-azure-event-hubs).

To create a service principal and assign RBAC roles to the application, review [How to Create a Service Principal](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal).


### Client Configuration

To connect a Kafka client to Event Hub using OAuth 2.0, the following configuration is required:

```properties
bootstrap.servers=<namespace>.servicebus.windows.net:9093
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
oauth_cb=<Callback for retrieving OAuth Bearer token> 
```

The return value of `oauth_cb` is expected to be a (token_str, expiry_time) tuple where expiry_time is the time in seconds since the epoch as a floating point number.
See [Confluent Kafka for Python](https://docs.confluent.io/platform/current/clients/confluent-kafka-python/html/index.html) for reference.

An example `oauth_cb` and kafka client configuration is defined below:

```python
from azure.identity import DefaultAzureCredential
from functools import partial

def oauth_cb(cred, namespace_fqdn, config):
    # note: confluent_kafka passes 'sasl.oauthbearer.config' as the config param
    access_token = cred.get_token('https://%s/.default' % namespace_fqdn)
    return access_token.token, access_token.expires_on

az_credential = DefaultAzureCredential()
eh_namespace_fqdn = '<namespace>.servicebus.windows.net'
kafka_conf = {
    'bootstrap.servers': '%s:9093' % namespace,
    'security.protocol': 'SASL_SSL',
    'sasl.mechanism': 'OAUTHBEARER',
    'oauth_cb': partial(oauth_cb, az_credential, eh_namespace_fqdn),
}

# Create Producer instance
p = Producer(kafka_conf)
```
