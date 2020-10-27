# Monitoring Java clients of Azure Event Hubs for Apache Kafka Ecosystems

This tutorial will be showing how to monitor a Java client application of Azure Event Hubs by using some suitable techniques. The approaches mentioned here will highlight the configuration changes to be made in order to track significant client metrics using Azure Monitor's Application Insights. Azure Event Hubs for Apache Kafka Ecosystems supports [Apache Kafka version 1.0](https://kafka.apache.org/10/documentation.html) and later. 

## Prerequisites

If you don't have an Azure subscription, create a [free account](https://azure.microsoft.com/free/?ref=microsoft.com&utm_source=microsoft.com&utm_medium=docs&utm_campaign=visualstudio) before you begin.

To complete this walkthrough, make sure you have the following prerequisites:

- [Java Development Kit (JDK) 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  - Be sure to set the JAVA_HOME environment variable to point to the folder where the JDK is installed.
- Go through [Azure Event Hubs for Kafka Java client samples](https://github.com/Azure/azure-event-hubs-for-kafka/tree/master/quickstart/java)
- Read [Azure Event Hubs Metrics in Azure Monitor](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-metrics-azure-monitor)
- Either use the Java client scripts from [here](https://www.apache.org/dyn/closer.cgi?path=/kafka/2.5.0/kafka_2.12-2.5.0.tgz) or use the code from [here](https://github.com/Azure/azure-event-hubs-for-kafka/tree/master/quickstart/java)

### Create an Event Hubs namespace

An Event Hubs namespace is required to send or receive from any Event Hubs service. See [Create Kafka-enabled Event Hubs](https://docs.microsoft.com/azure/event-hubs/event-hubs-create-kafka-enabled) for instructions on getting an Event Hubs Kafka endpoint. Make sure to copy the Event Hubs connection string for later use.

### FQDN

For this sample, you will need the connection string from the portal as well as the FQDN that points to your Event Hub namespace. **The FQDN can be found within your connection string as follows**:

```
Endpoint=sb://`**`mynamespace.servicebus.windows.net`**`/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX
```

If your Event Hubs namespace is deployed on a non-Public cloud, your domain name may differ (e.g. *.servicebus.chinacloudapi.cn, *.servicebus.usgovcloudapi.net, or *.servicebus.cloudapi.de).

### Create an Application Insights Resource

In order to track the metrics in real-time on Azure Monitor, it is essential to have an Application Insights Resource created. Also note down the the Instrumentation Key of this resource. For instructions, refer to this article: [Creating an Appliction Insights Resource](https://docs.microsoft.com/en-us/azure/azure-monitor/app/create-new-resource).

## Overview 

For EH clients that are Java based, this document outlines an approach to monitor some client metrics, with the help of JMX. The monitoring will be done using Azure Monitor’s Application Insights. For this, we are recommending making use of the Application Insights Java 3.0 Preview. This is an agent which auto collects requests, dependencies and logs from the Java apps running anywhere on VMs, AKS, on-premises etc. We will further enable JMX configurations on this agent and send some metrics to the Application Insights Resource. 

Start by downloading the agent from [here](https://github.com/Microsoft/ApplicationInsights-Java/releases/tag/3.0.0-PREVIEW.7). Further details regarding how to use the agent can be found [here](https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent). 

Place the agent in the same directory as the client application that you are running. Now create a file named `ApplicationInsights.json` in the same directory. This provides some configuration option for the agent. Firstly, configure the agent to point to the Application Insights Resource as shown below:

```json
{
  "instrumentationSettings": {
    "connectionString": "InstrumentationKey=00000000-0000-0000-0000-000000000000"
  }
}
```

where the `connectionString` is the `InstrumentationKey` of the resource previously created. 

Now, we need to enable JMX configuration on this agent. This will tell the agent which attributes should be sent to the configured resource. In order to fill in with the `objectName` and `attribute` fields, we will connect to it later using *JConsole*. This will also help us in determining some of the most significant attributes. In order to configure the attributes, add the following lines to the `ApplicationInsights.json` file. You can find the entire file in the config directory, which can be used along with both the producer and the consumer applications.

```json
"preview": { 
    "jmxMetrics": [ 
        { 
            "objectName": "kafka.consumer:type=consumer-fetch-manager-metrics,client-id=consumer-testgroup-1", 
            "attribute": "records-lag-max", 
            "display": "Records Lag Max" 
        }, 
        { 
            "objectName": "kafka.consumer:type=consumer-fetch-manager-metrics,client-id=consumer-testgroup-1", 
            "attribute": "records-consumed-rate", 
            "display": "Records consumed Rate" 
        }, 
    ] 
} 
```

The agent now will be able to collect the two JMX attributes shown above, namely the records-lag-max, which is the consumer lag and the records-consumed-rate.

You can add additional metrics to the jmxMetrics list. There is also an additional field called heartbeat which is the frequency at which the metrics will be sent to the Application Insights Resource. A good choice would be 10 seconds. Choosing a low heartbeat interval provides high fidelity monitoring but can result in a large AI bill.

```json
"heartbeat": { 
     "intervalSeconds": 10 
 } 
```

Make sure the value is suitably configured so that the rate at with the metric updates are sent is appropriate for your application.  Before running the Java consumer application, make sure that the following environmental variables are set. Ensure that the correct agent version is used.

```
SetLocal
set "KAFKA_JMX_OPTS=-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=127.0.1.1 -Dcom.sun.management.jmxremote.rmi.port=9010"
set "KAFKA_HEAP_OPTS=-Xmx512M"
set "KAFKA_JVM_PERFORMANCE_OPTS=-javaagent:applicationinsights-agent-3.0.0-PREVIEW.7.jar"
"%~dp0kafka-run-class.bat" kafka.tools.ConsoleConsumer %*
EndLocal
```

In case you are running a kafka-console-consumer script application provided by Apache, you can add the above the lines directly to it. Otherwise set them from the command line. In KAFKA_JMX_OPTS, we enable JMX and expose it via port 9010 on the local machine. In KAFKA_JVM_PERFORMANCE_OPTS, we point the application to the location of the Application Insights agent.

The JMX is now enabled on running the consumer application. You can now connect to it using JConsole and view some of the metrics that are available. For the consumer application, the below metrics are recommended for tracking:

| Consumer Metrics      | Producer Metrics     |
| --------------------- | -------------------- |
| records-lag           | request-rate         |
| records-lag-max       | response-rate        |
| bytes-consumed-rate   | compression-rate-avg |
| records-consumed-rate | request-latency-avg  |
| fetch-rate            | batch-size-avg       |
|                       | outgoing-byte-avg    |
|                       | io-wait-time-ns-avg  |

The metric will be sent to the configured Application Insights Resource. It can be viewed under: Application Insights Resource -> Monitoring -> Metrics -> Custom [azure.application.insights]. 

![Visualization of Bytes Consumed Total Attribute](https://user-images.githubusercontent.com/73145416/97199086-fc8b2a00-1785-11eb-9606-993b7a7c292a.PNG)

## References

The below links can be used as quick references:

- [Configuring JVM args Java standalone agent for Azure Monitor Application Insights](https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-standalone-arguments)

- [Azure Event Hubs Metrics in Azure Monitor](https://docs.microsoft.com/en-us/azure/event-hubs/event-hubs-metrics-azure-monitor)

