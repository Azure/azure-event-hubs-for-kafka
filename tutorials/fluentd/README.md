# Apache Kafka Output Plugins for FluentD

This document will walk you through integrating Fluentd and Event Hubs using the `out_kafka` output plugin for Fluentd.

## Prerequisites

To complete this walkthough, make sure you have the following prerequisites:

- FluentD (https://docs.fluentd.org/v0.12/categories/installation)

- `out_kafka` plugin for FluentD (the plugin is included with `td-agent2 v2.3.3+` or `td-agent3`; gem users should run `fluent-gem install fluent-plugin-kafka`)

- Read through the [Event Hubs for Apache Kafka](https://docs.microsoft.com/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview) introduction article

- Created a [Kafka-enabled Event Hubs namespace](https://docs.microsoft.com/azure/event-hubs/event-hubs-create) on the Azure Portal

## Overview

FluentD is a free open-source data collector that enables easy configuration-driven log streaming to and from over six hundred data sources and sinks using community-developed plugins.

Many FluentD users employ the `out_kafka` plugin to move data to an Apache Kafka cluster for deferred processing.  The same `out_kafka` plugin can be reconfigured to stream logs to the Kafka protocol endpoint on Azure Event Hubs, a managed, pay-as-you-go messaging service guaranteeing high throughput and reliability.

## FluentD configuration sample

The following sample matches any logs from sources with a `kafka.*` tag.

```yaml
<match kafka.**>
  @type kafka_buffered

  # list of seed brokers
  brokers {YOUR.EVENTHUBS.FQDN}:9093

  # buffer settings
  buffer_type file
  buffer_path /var/log/td-agent/buffer/kafka
  flush_interval 3s

  # topic settings
  default_topic TOPIC_NAME

  # data type settings
  output_data_type json

  # producer settings
  max_send_retries 1
  required_acks -1

  # using OS certs - 
  ssl_ca_certs_from_system true
  username $ConnectionString
  password "Endpoint=sb://{YOUR.EVENTHUBS.FQDN}/;SharedAccessKeyName={SHARED.ACCESS.KEY.NAME};SharedAccessKey={SHARED.ACCESS.KEY}"
</match>
```

Note: the above configuration assumes that 