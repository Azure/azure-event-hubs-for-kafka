/* Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2016 Blizzard Entertainment
 * Licensed under the MIT License.
 *
 * Original Blizzard node-rdkafka sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems
 */
 
var Transform = require('stream').Transform;
var Kafka = require('node-rdkafka');

var stream = Kafka.KafkaConsumer.createReadStream({
    'metadata.broker.list': 'mynamespace.servicebus.windows.net:9093', //REPLACE
    'group.id': '$Default', //The default consumer group for EventHubs is $Default
    'socket.keepalive.enable': true,
    'enable.auto.commit': false,
    'security.protocol': 'SASL_SSL',
    'sasl.mechanisms': 'PLAIN',
    'sasl.username': '$ConnectionString', //do not replace $ConnectionString
    'sasl.password': 'Endpoint=sb://mynamespace.servicebus.windows.net/;SharedAccessKeyName=XXXXXX;SharedAccessKey=XXXXXX', //REPLACE

}, {}, {
        topics: 'test',
        waitInterval: 0,
        objectMode: false
    });

stream.on('error', function (err) {
    if (err) console.log(err);
    process.exit(1);
});

stream
    .pipe(process.stdout);

stream.on('error', function (err) {
    console.log(err);
    process.exit(1);
});

stream.consumer.on('event.error', function (err) {
    console.log(err);
})
