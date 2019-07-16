

https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-starting.html

https://www.elastic.co/guide/en/beats/filebeat/master/kafka-output.html

# Apache Kafka Output for Elastic Filebeat

This document will walk you through integrating Filebeat and Event Hubs via Filebeat's Kafka output.

## Prerequisites

To complete this walkthough, make sure you have the following prerequisites:

- Filebeat (https://www.elastic.co/guide/en/beats/filebeat/current/filebeat-getting-started.html)

- Read through the Kafka to Filebeat tutorial (https://www.elastic.co/guide/en/beats/filebeat/master/kafka-output.html)

- Read through the [Event Hubs for Apache Kafka](https://docs.microsoft.com/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview) introduction article

- Created a [Kafka-enabled Event Hubs namespace](https://docs.microsoft.com/azure/event-hubs/event-hubs-create) on the Azure Portal

## Overview

Filebeat serves as a lightweight log forwarding agent that monitors specific files on your servers and forwards new events to Logstash or ElasticSearch for deferred processing.

Filebeat supports outputting log data to Kafka over SSL via SASL/PLAIN authentication, making Event Hubs with Kafka an ideal aggregation mechanism for your production workloads. 

## Filebeat sample configuration

The following Filebeat configuration sample demonstrates correct SASL/PLAIN authentication for EH Kafka.

```yaml
filebeat.inputs:
- input_type: log
  paths:
    - /log/file/location
output.kafka:
  topic: topic_name
  required_acks: 1
  client_id: filebeat
  version: '1.0.0'
  hosts:
    - "mynamespace.servicebus.windows.net:9093"
  username: "$ConnectionString"
  password: "Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXXXXX;SharedAccessKey=XXXXXXXXXXX"
  ssl.enabled: true
  compression: none
  max_message_bytes: 1000000
  required_acks: 1
  partition.round_robin:
    reachable_only: true
logging.level: debug
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0644
```