# Recommended configurations

SASL and SSL settings are not included here.  Please refer to SDK quickstarts.

## Java client configuration properties



### Producer and consumer configurations

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
metadata.max.age.ms | ~ 180000 | < 240000 | Can be lowered to pick up metadata changes sooner.
connections.max.idle.ms	| 180000 | < 240000 | Azure closes inbound TCP idle > 240000 ms, which can result in sending on dead connections (shown as expired batches due to send timeout).

### Producer configurations only
Producer configs can be found [here](https://kafka.apache.org/documentation/#producerconfigs).

Property | Recommended Values | Permitted Range | Notes
---|---:|---:|---
max.request.size | 1000000 | < 1046528 | The service will close connections if requests larger than 1046528 bytes are sent.  *This **must** be changed and will cause issues in high-throughput produce scenarios.*
retries | > 0 | | May require increasing delivery.timeout.ms value, see documentation.
request.timeout.ms | 30000 .. 60000 | > 20000| EH will internally default to a minimum of 20000 ms.  *While requests with lower timeout values are accepted, client behavior is not guaranteed.*
metadata.max.idle.ms | 180000 | > 5000 | Controls how long the producer will cache metadata for a topic that's idle. If the elapsed time since a topic was last produced to exceeds the metadata idle duration, then the topic's metadata is forgotten and the next access to it will force a metadata fetch request.
linger.ms | > 0 | | For high throughput scenarios, linger value should be equal to the highest tolerable value to take advantage of batching.
delivery.timeout.ms | | | Set according to the formula (`request.timeout.ms` + `linger.ms`) * `retries`.
enable.idempotence | false | | Idempotency currently not supported.
compression.type | `none` | | Compression currently not supported..

### Consumer configurations only
Consumer configs can be found [here](https://kafka.apache.org/documentation/#consumerconfigs).

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
heartbeat.interval.ms | 3000 | | This is default and should not be changed.
session.timeout.ms | 30000 |6000 .. 300000| Start with 30000, increase if seeing frequent rebalancing due to missed heartbeats.


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
retries | > 0 | | Default is 2. This is fine.
request.timeout.ms | 30000 .. 60000 | > 20000| EH will internally default to a minimum of 20000 ms.  `librdkafka` default value is 5000, which can be problematic. *While requests with lower timeout values are accepted, client behavior is not guaranteed.*
partitioner | `consistent_random` | See librdkafka documentation | `consistent_random` is default and best.  Empty and null keys are handled ideally for most cases.
enable.idempotence | false | | Idempotency currently not supported.
compression.codec | `none` || Compression currently not supported.

### Consumer configurations only

Property | Recommended Values | Permitted Range | Notes
---|---:|-----:|---
heartbeat.interval.ms | 3000 || This is default and should not be changed.
session.timeout.ms | 30000 |6000 .. 300000| Start with 30000, increase if seeing frequent rebalancing due to missed heartbeats.


## Further notes

Check the following table of common configuration-related error scenarios.

Symptoms | Problem | Solution
----|---|-----
Offset commit failures due to rebalancing | Your consumer is waiting too long in between calls to poll() and the service is kicking the consumer out of the group. | You have several options: <ul><li>increase session timeout</li><li>decrease message batch size to speed up processing</li><li>improve processing parallelization to avoid blocking consumer.poll()</li></ul> Applying some combination of the three is likely wisest.
Network exceptions at high produce throughput | Are you using Java client + default max.request.size?  Your requests may be too large. | See Java configs above.
