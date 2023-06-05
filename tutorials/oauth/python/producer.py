#!/usr/bin/env python
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Copyright 2023 Confluent Inc.
# Licensed under the MIT License.
# Licensed under the Apache License, Version 2.0
#
# Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

from confluent_kafka import Producer
import sys
import argparse
import config_utils


def produce_workload(conf, topic, num_messages):
    # Create Producer instance
    p = Producer(**conf)


    # Optional per-message delivery callback (triggered by poll() or flush())
    # when a message has been successfully delivered or permanently
    # failed delivery (after retries).
    def delivery_callback(err, msg):
        if err:
            sys.stderr.write('%% Message failed delivery: %s\n' % err)
        else:
            sys.stderr.write('%% Message delivered to %s [%d] @ %d\n' %
                                (msg.topic(), msg.partition(), msg.offset()))

    # Write 1-num_messages to topic
    for i in range(num_messages):
        try:
            p.produce(topic, str(i), callback=delivery_callback)
        except BufferError:
            sys.stderr.write('%% Local producer queue is full (%d messages awaiting delivery): try again\n' %
                             len(p))

        # Serve delivery callback queue.
        # NOTE: Since produce() is an asynchronous API this poll() call
        #       will most likely not serve the delivery callback for the
        #       last produce()d message.
        p.poll(0)

    # Wait until all messages have been delivered
    sys.stderr.write('%% Waiting for %d deliveries\n' % len(p))
    p.flush()


def parse_producer_args():
    parser = argparse.ArgumentParser(description='Process command line arguments.')
    parser.add_argument('namespace', help='Eventhubs namespace')
    parser.add_argument('topic', help='Topic or Event Hub')
    parser.add_argument('--mode', default='azure',
                        choices=['azure', 'oidc', 'opaque'],
                        help='Token request callback implementation logic')
    parser.add_argument('--num-messages', type=int, default=100, 
                        help='Number of messages to be produced')

    args = parser.parse_args()
    return args.namespace, args.topic, args.mode, args.num_messages


if __name__ == '__main__':
    namespace, topic, mode, num_messages = parse_producer_args()

    conf = config_utils.get_config(namespace, mode)

    produce_workload(conf, topic, num_messages=num_messages)