# Recommended configurations

SASL and SSL settings are not included here.  Please refer to SDK quickstarts.

## Java client configuration properties



### Producer and consumer configurations

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
metadata.max.age.ms | ~ 180000 | < 240000 | Can be lowered to pick up metadata changes sooner.
connections.max.idle.ms	| 180000 | < 240000 | Azure closes inbound TCP idle > 240000 ms, which can result in sending on dead connections (shown as expired batches due to send timeout).

### Producer configurations only
Producer configs can be found [here](https://docs.confluent.io/current/installation/configuration/producer-configs.html).

Property | Recommended Values | Permitted Range | Notes
---|---:|---:|---
max.request.size | 1046528 | | 
request.timeout.ms | 30000 .. 60000 | > 20000| EH will internally default to a minimum of 20000 ms.
enable.idempotence | false | | Idempotency currently not supported.
compression.type | `none` | | Compression currently not supported..

### Consumer configurations only
Consumer configs can be found [here](https://docs.confluent.io/current/installation/configuration/consumer-configs.html).

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
heartbeat.interval.ms | 3000 | | This is default and should not be changed.
session.timeout.ms | 20000 |6000 .. 300000| Start with 20000, increase if seeing frequent rebalancing due to missed heartbeats.


## librdkafka configuration properties
The main `librdkafka` configuration file ([link](https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md)) contains extended descriptions for the properties below.

### Producer and consumer configurations

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
socket.keepalive.enable | true | | Necessary if connection is expected to idle.  Azure will close inbound TCP idle > 240000 ms.
metadata.max.age.ms | ~ 180000| < 240000 | Can be lowered to pick up metadata changes sooner.

### Producer configurations only

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
request.timeout.ms | 30000 .. 60000 | > 20000| EH will internally default to a minimum of 20000 ms.  `librdkafka` default value is 5000, which can be problematic.
enable.idempotence | false | | Idempotency currently not supported.
compression.codec | `none` || Compression currently not supported.

### Consumer configurations only

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
heartbeat.interval.ms | 3000 || This is default and should not be changed.
session.timeout.ms | 20000 |6000 .. 300000| Start with 20000, increase if seeing frequent rebalancing due to missed heartbeats.