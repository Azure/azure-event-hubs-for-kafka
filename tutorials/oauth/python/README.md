# Using Azure Active Directory authentication with Python and Event Hubs for Apache Kafka

This tutorial will show how to create and connect to an Event Hubs Kafka endpoint using Azure Active Directory authentication. Azure Event Hubs for Apache Kafka supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later.

This sample is based on [Confluent's Apache Kafka Python client](https://github.com/confluentinc/confluent-kafka-python), modified for use with Event Hubs for Kafka.

## Overview

Event Hubs integrates with Azure Active Directory (Azure AD), which provides an OAuth 2.0 compliant authorization server. Azure role-based access control (Azure RBAC) can be used to grant permissions to Kafka client identities.

To grant access to an Event Hubs resource, the security principal must be authenticated and an OAuth 2.0 token is returned. The token is then passed as part of the request to the Event Hubs service to authorize access to the resource. This is done by setting the appropriate values in the Kafka client configuration.

For more information on using Azure AD with Event Hubs, see [Authorize access with Azure Active Directory](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory#overview)

### Retrieve Azure Active Directory (AAD) Token

The `DefaultAzureCredential` class from the [Azure Identity client library](https://docs.microsoft.com/en-us/python/api/overview/azure/identity-readme?view=azure-python) can be used to get a credential with the scope of `https://<namespace>.servicebus.windows.net/.default` to retrieve the access token for the Event Hubs namespace.

This class is suitable for use with Azure CLI for local development, Managed Identity for Azure deployments, and with service principal client secrets/certificates. See [DefaultAzureCredential](https://docs.microsoft.com/en-us/python/api/azure-identity/azure.identity.defaultazurecredential?view=azure-python) for more information on this class.

### Role Assignments

An RBAC role must be assigned to the application to gain access to Event Hubs. Azure provides built-in roles for authorizing access to Event Hubs. See [Azure Built in Roles for Azure Event Hubs](https://docs.microsoft.com/en-us/azure/event-hubs/authorize-access-azure-active-directory#azure-built-in-roles-for-azure-event-hubs).

To create a service principal and assign RBAC roles to the application, review [How to Create a Service Principal](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal).

### Kafka Client Configuration

To connect a Kafka client to Event Hubs using OAuth 2.0, the following configuration properties are required:

```properties
bootstrap.servers=<namespace>.servicebus.windows.net:9093
security.protocol=SASL_SSL
sasl.mechanism=OAUTHBEARER
oauth_cb=<Callback for retrieving OAuth Bearer token> 
```

The return value of `oauth_cb` must be a (`token_str`, `expiry_time`) tuple where `expiry_time` is the time in seconds since the epoch as a floating point number.
See [Confluent Kafka for Python](https://docs.confluent.io/platform/current/clients/confluent-kafka-python/html/index.html) for reference.

An example `oauth_cb` and kafka client configuration is shown below:

```python
from azure.identity import DefaultAzureCredential
from confluent_kafka import Producer
from functools import partial

def oauth_cb(cred, namespace_fqdn, config):
    # note: confluent_kafka passes 'sasl.oauthbearer.config' as the config param
    access_token = cred.get_token(f'https://{namespace_fqdn}/.default')
    return access_token.token, access_token.expires_on

# A credential object retrieves access tokens
credential = DefaultAzureCredential()

# The namespace FQDN is used both for specifying the server and as the token audience
namespace_fqdn = '<namespace>.servicebus.windows.net'

# Minimum required configuration to connect to Event Hubs for Kafka with AAD
p = Producer({
    'bootstrap.servers': f'{namespace_fqdn}:9093',
    'security.protocol': 'SASL_SSL',
    'sasl.mechanism': 'OAUTHBEARER',
    'oauth_cb': partial(oauth_cb, credential, namespace_fqdn),
})
```

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

You'll also need:

* [Git](https://www.git-scm.com/downloads)
* [Python](https://www.python.org/downloads/)
* [Pip](https://pypi.org/project/pip/)

## Create an Event Hubs namespace

An Event Hubs namespace includes a Kafka endpoint and is required to send or receive data. See [Quickstart: Create an event hub using Azure portal](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-create) for instructions on creating an Event Hubs namespace.

## Getting ready

Now that you have an Event Hubs namespace, clone the repository and navigate to the `tutorials/oauth/python` directory:

```bash
git clone https://github.com/Azure/azure-event-hubs-for-kafka.git
cd azure-event-hubs-for-kafka/tutorials/oauth/python
```

Install the sample dependencies:

```shell
python -m pip install -r requirements.txt
```

## Running the samples

### Configuring credentials

In this example, a service principal is used for the AAD credential. This service principal must have the `Azure Event Hubs Data Sender` and `Azure Event Hubs Data Receiver` roles (or equivalent) assigned on the target Event Hubs namespace. We configure `DefaultAzureCredential` by setting the following environment variables:

```shell
export AZURE_TENANT_ID=<TenantID>
export AZURE_CLIENT_ID=<AppClientId>
export AZURE_CLIENT_SECRET=<AppSecret>
```

### Producing

```shell
# Usage: producer.py <eventhubs-namespace> <topic>.
python producer.py mynamespace.servicebus.windows.net topic1
```

> Note: the topic must already exist, or will see an "Unknown topic or partition" error when running with the `Azure Event Hubs Data Sender` role. With the `Azure Event Hubs Data Owner` role, the topic (Event Hub) will be automatically created.

### Consuming

```shell
# Usage: consumer.py [options..] <eventhubs-namespace> <group> <topic1> <topic2> ..
python consumer.py mynamespace.servicebus.windows.net myconsumergroup topic1
```
