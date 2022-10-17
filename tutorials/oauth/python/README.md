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

## Troubleshooting

Listed below are some common errors and possible remediations

### Missing credentials

```text
DefaultAzureCredential failed to retrieve a token from the included credentials.
Attempted credentials:
        EnvironmentCredential: EnvironmentCredential authentication unavailable. Environment variables are not fully configured.
Visit https://aka.ms/azsdk/python/identity/environmentcredential/troubleshoot to troubleshoot.this issue.
        ManagedIdentityCredential: ManagedIdentityCredential authentication unavailable, no response from the IMDS endpoint.
        SharedTokenCacheCredential: SharedTokenCacheCredential authentication unavailable. No accounts were found in the cache.
        VisualStudioCodeCredential: Failed to get Azure user details from Visual Studio Code.
        AzureCliCredential: Azure CLI not found on path
        AzurePowerShellCredential: PowerShell is not installed
To mitigate this issue, please refer to the troubleshooting guidelines here at https://aka.ms/azsdk/python/identity/defaultazurecredential/troubleshoot.
```

#### Cause

You are missing credentials to connect to Azure AD and retrieve an access token.

#### Remediation

* If using the [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/install-azure-cli), run `az login`
* If using a service principal, set the following environment variables:
  * `AZURE_TENANT_ID`
  * `AZURE_CLIENT_ID`
  * `AZURE_CLIENT_SECRET`

### Incorrect credentials

```text
DefaultAzureCredential failed to retrieve a token from the included credentials.
Attempted credentials:
        EnvironmentCredential: Authentication failed: AADSTS7000215: Invalid client secret provided. Ensure the secret being sent in the request is the client secret value, not the client secret ID, for a secret added to app '11dba002-88d3-45a5-b8c0-75a84ec2c1eb'.
Trace ID: ba773467-5755-4492-9c05-d35d73683600
Correlation ID: e976bf48-228a-4114-b06e-ce97aeb52af3
Timestamp: 2022-09-28 16:43:50Z
To mitigate this issue, please refer to the troubleshooting guidelines here at https://aka.ms/azsdk/python/identity/defaultazurecredential/troubleshoot.
```

#### Cause

Your service principal credentials are incorrect or expired

#### Remediation

Ensure that your `AZURE_CLIENT_ID` and `AZURE_CLIENT_SECRET` values are correct

### Missing role assignments

Producer:

```text
% Message failed delivery: KafkaError{code=TOPIC_AUTHORIZATION_FAILED,val=29,str="Broker: Topic authorization failed"}
```

Consumer:

```text
cimpl.KafkaException: KafkaError{code=TOPIC_AUTHORIZATION_FAILED,val=29,str="Failed to fetch committed offset for group "<consumer group>" topic <topic name>[0]: Broker: Topic authorization failed"}
```

#### Cause

You are missing Azure RBAC role assignments for your application's identity (Azure CLI, Service Principal, etc)

#### Remediation

* Ensure that your application's identity is assigned the correct roles
  * Producers need `Azure Event Hubs Data Sender`
  * Consumers need `Azure Event Hubs Data Receiver`
