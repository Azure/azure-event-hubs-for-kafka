/* Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2016 Blizzard Entertainment
 * Licensed under the MIT License.
 *
 * Original Blizzard node-rdkafka sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems
 */
var Transform = require('stream').Transform;
var Kafka = require('node-rdkafka');
require('dotenv').config();

var stream = Kafka.KafkaConsumer.createReadStream({
    'metadata.broker.list': `${process.env.CONNECTION_STRING.split('/')[2]}:9093`,
    'group.id': '$Default', //The default consumer group for EventHubs is $Default
    'socket.keepalive.enable': true,
    'enable.auto.commit': false,
    'security.protocol': 'SASL_SSL',
    'sasl.mechanisms': 'PLAIN',
    'sasl.username': '$ConnectionString', //do not replace $ConnectionString
    'sasl.password': `${process.env.CONNECTION_STRING}`,
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
