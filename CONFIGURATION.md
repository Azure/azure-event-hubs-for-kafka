# Recommended configurations

SASL and SSL settings are not included here.  Please refer to SDK quickstarts.

## Java client configuration properties

Property | Client Type | Recommended Value/Range | Permitted Range | Notes
---|---|---:|-----:|---
metadata.max.age.ms | all clients| ~ 180000 | < 240000 | Can be lowered to pick up metadata changes sooner.
connections.max.idle.ms	| all clients | 180000 | < 240000 | Azure closes inbound TCP idle > 240000 ms, which can result in sending on dead connections (shown as expired batches due to send timeout).
max.request.size | producer | 1046528‬ | | 
request.timeout.ms |  producer | 30000 .. 60000 || EH will default to a minimum of 20000 ms.
enable.idempotence	| producer | false | | Idempotence currently not supported.
compression.type | producer | `none` | | Compression currently not supported..
heartbeat.interval.ms | consumer | 3000 || This is default and should not be changed.
session.timeout.ms	| consumer | 20000 |6000 .. 300000| Start with 20000, increase if seeing frequent rebalancing due to missed heartbeats.


## librdkafka configuration properties
The main `librdkafka` configuration file ([link](https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md)) contains extended descriptions for the properties below.

Property | Client Type | Recommended Value/Range | Permitted Range | Notes
---|---|---:|-----:|---
socket.keepalive.enable | all clients | true | | Necessary if connection is expected to idle.  Azure will close inbound TCP idle > 240000 ms.
metadata.max.age.ms	| all clients | ~ 180000| < 240000 | Can be lowered to pick up metadata changes sooner
request.timeout.ms |  producer | 30000 .. 60000 || EH will default to a minimum of 20000 ms.
enable.idempotence | producer | false | | Idempotence currently not supported.
compression.codec | producer | `none` || Compression currently not supported.
heartbeat.interval.ms | consumer | 3000 || This is default and should not be changed.
session.timeout.ms	| consumer | 20000 |6000 .. 300000| Start with 20000, increase if seeing frequent rebalancing due to missed heartbeats.