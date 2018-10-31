# Apache Kafka Connect support on Azure Event Hubs

This document will walk you through integrating Kafka Connect with Azure Event Hubs and deploying basic FileStreamSource and FileStreamSink connectors.

## Prerequisites

To complete this walkthough, make sure you have the following prerequisites:

- Linux/MacOS

- Kafka release (version 1.1.1, Scala version 2.11), available from [kafka.apache.org](https://kafka.apache.org/downloads#1.1.1)

- Read through the [Event Hubs for Apache Kafka](https://docs.microsoft.com/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview) introduction article

- Created a [Kafka-enabled Event Hubs namespace](https://docs.microsoft.com/azure/event-hubs/event-hubs-create) on the Azure Portal

## Key Concepts

[Kafka Connect](https://kafka.apache.org/documentation/#connect) is a platform that provides easy integration of Apache Kafka and other services into secure and scalable pipelines.  Workers can be deployed in standalone mode (single processes) or distributed mode (nodes in a cluster of Connect workers).  Source and sink connectors are used to define movement of data in and out of the Kafka cluster respectively.

More information on Kafka Connect concepts is available [here](https://docs.confluent.io/current/connect/concepts.html).

## Configuring Kafka Connect for Event Hubs

Minimal reconfiguration is necessary when redirecting Kafka Connect throughput from Kafka to Event Hubs.  The following `connect-distributed.properties` sample illustrates how to configure Connect to authenticate and communicate with the Kafka endpoint on Event Hubs:

```properties
bootstrap.servers={YOUR.EVENTHUBS.FQDN}:9093 # e.g. namespace.servicebus.windows.net:9093
group.id=connect-cluster-group

# connect internal topic names, auto-created if not exists
config.storage.topic=connect-cluster-configs
offset.storage.topic=connect-cluster-offsets
status.storage.topic=connect-cluster-status

# internal topic replication factors - auto 3x replication in Azure Storage
config.storage.replication.factor=1
offset.storage.replication.factor=1
status.storage.replication.factor=1

rest.advertised.host.name=connect
offset.flush.interval.ms=10000

key.converter=org.apache.kafka.connect.json.JsonConverter
value.converter=org.apache.kafka.connect.json.JsonConverter
internal.key.converter=org.apache.kafka.connect.json.JsonConverter
internal.value.converter=org.apache.kafka.connect.json.JsonConverter

internal.key.converter.schemas.enable=false
internal.value.converter.schemas.enable=false

# required EH Kafka security settings
security.protocol=SASL_SSL
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="{YOUR.EVENTHUBS.CONNECTION.STRING}";

producer.security.protocol=SASL_SSL
producer.sasl.mechanism=PLAIN
producer.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="{YOUR.EVENTHUBS.CONNECTION.STRING}";

consumer.security.protocol=SASL_SSL
consumer.sasl.mechanism=PLAIN
consumer.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="{YOUR.EVENTHUBS.CONNECTION.STRING}";

plugin.path={KAFKA.DIRECTORY}/libs # path to the libs directory within the Kafka release
```

## Running Kafka Connect

In this step, a Kafka Connect worker will be started locally in distributed mode, using Event Hubs to maintain cluster state.

1. Save the above `connect-distributed.properties` file locally.  Be sure to replace all values in braces.

2. Navigate to the location of the Kafka release on your machine.

3. Run `./bin/connect-distributed.sh /PATH/TO/connect-distributed.properties`.  The Connect worker REST API is ready for interaction when you see `'INFO Finished starting connectors and tasks'`. 

### Creating Connectors

This section will walk you through spinning up FileStreamSource and FileStreamSink connectors. 

1. Create a directory for our input and output data files to live
    ```bash
    mkdir ~/connect-quickstart
    ```

2. Create two files: one file with seed data from which the FileStreamSource connector will read, and another to which our FileStreamSink connector will write.
    ```bash
    seq 1000 > ~/connect-quickstart/input.txt
    touch ~/connect-quickstart/output.txt
    ```

3. Create a FileStreamSource connector.  Be sure to replace the curly braces with your home directory path.
    ```bash
    curl -s -X POST -H "Content-Type: application/json" --data '{"name": "file-source","config": {"connector.class":"org.apache.kafka.connect.file.FileStreamSourceConnector","tasks.max":"1","topic":"connect-quickstart","file": "{YOUR/HOME/PATH}/connect-quickstart/input.txt"}}' http://localhost:8083/connectors
    ```

    Note: **Event Hubs supports Kafka's topic auto-creation!**  You should see the Event Hub `connect-quickstart` on your Event Hubs instance after running the above command.

4. Check status of source connector
    ```bash
    curl -s http://localhost:8083/connectors/file-source/status
    ```
    Optionally, you can use [Service Bus Explorer](https://github.com/paolosalvatori/ServiceBusExplorer/releases) to verify that events have arrived in the `connect-quickstart` topic.

5. Create a FileStreamSink Connector.  Again, make sure you replace the curly braces with your home directory path.
    ```bash
    curl -X POST -H "Content-Type: application/json" --data '{"name": "file-sink", "config": {"connector.class":"org.apache.kafka.connect.file.FileStreamSinkConnector", "tasks.max":"1", "topics":"connect-quickstart", "file": "{YOUR/HOME/PATH}/connect-quickstart/output.txt"}}' http://localhost:8083/connectors
    ```
 
6. Check the status of sink connector.
    ```bash
    curl -s http://localhost:8083/connectors/file-sink/status
    ```

7. Verify that data has been replicated between files and that the data is identical across both files.
    ```bash
    # read the file
    cat ~/connect-quickstart/output.txt
    # diff the input and output files
    diff ~/connect-quickstart/input.txt ~/connect-quickstart/output.txt
    ```

### Cleanup

Kafka Connect will create Event Hub topics to store configurations, offsets, and status that persist even after the Connect cluster has been taken down.  Unless this persistence is desired, it is recommended that these topics are deleted.  You may also want to delete the `connect-quickstart` Event Hub that were created during the course of this walkthrough.

You can use any of the following tools to manage your Event Hubs namespace:

- [Azure portal](portal.azure.com) (Note: consumer groups auto-created by Kafka clients will not appear on the Event Hubs consumer group lists.)

- [Azure CLI](https://docs.microsoft.com/en-us/cli/azure/eventhubs?view=azure-cli-latest)

- [C# Namespace Manager library](https://docs.microsoft.com/en-us/dotnet/api/microsoft.servicebus.namespacemanager?view=azure-dotnet)

- [Service Bus Explorer](https://github.com/paolosalvatori/ServiceBusExplorer/releases)
