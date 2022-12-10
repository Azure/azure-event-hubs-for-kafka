#!/usr/bin/env python
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Copyright 2023 Confluent Inc.
# Licensed under the MIT License.
# Licensed under the Apache License, Version 2.0
#
# Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

from confluent_kafka import Consumer, KafkaException
import sys
import argparse
import json
import logging
from pprint import pformat
import config_utils


def consume_workload(conf, topics):
    # Create logger for consumer (logs will be emitted when poll() is called)
    logger = logging.getLogger('consumer')
    logger.setLevel(logging.DEBUG)
    handler = logging.StreamHandler()
    handler.setFormatter(logging.Formatter('%(asctime)-15s %(levelname)-8s %(message)s'))
    logger.addHandler(handler)

    # Create Consumer instance
    # Hint: try debug='fetch' to generate some log messages
    c = Consumer(conf, logger=logger)

    def print_assignment(consumer, partitions):
        print('Assignment:', partitions)

    # Subscribe to topics
    c.subscribe(topics, on_assign=print_assignment)

    # Read messages from Kafka, print to stdout
    try:
        while True:
            msg = c.poll(timeout=1.0)
            if msg is None:
                continue
            if msg.error():
                raise KafkaException(msg.error())
            else:
                # Proper message
                sys.stderr.write('%% %s [%d] at offset %d with key %s:\n' %
                                 (msg.topic(), msg.partition(), msg.offset(),
                                  str(msg.key())))
                print(msg.value())

    except KeyboardInterrupt:
        sys.stderr.write('%% Aborted by user\n')

    finally:
        # Close down consumer to commit final offsets.
        c.close()


def parse_consumer_args():
    parser = argparse.ArgumentParser(description='Process command line arguments.')
    parser.add_argument('namespace', help='Eventhubs namespace')
    parser.add_argument('group', help='Group')
    parser.add_argument('topics', nargs='+', help='Topic1, Topic2, ...')
    parser.add_argument('-T', type=int, help='Enable client statistics at specified interval (ms)')
    parser.add_argument('--mode', default='azure', choices=['azure', 'oidc', 'opaque'], help='Optional confluent producer configuration mode - azure, oidc, opaque')

    args = parser.parse_args()

    if args.T and args.T <= 0:
        sys.stderr.write("-T option value needs to be larger than zero: %s\n" % args.T)
        sys.exit(1)

    return args.namespace, args.group, args.topics, args.T, args.mode


if __name__ == '__main__':
    namespace, group, topics, T, mode = parse_consumer_args()

    conf = config_utils.get_config(namespace, mode)
    conf.update({
        'group.id': group,
        'session.timeout.ms': 6000,
        'auto.offset.reset': 'earliest'
    })

    def stats_cb(stats_json_str):
        stats_json = json.loads(stats_json_str)
        print('\nKAFKA Stats: {}\n'.format(pformat(stats_json)))

    if T:
        conf['stats_cb'] = stats_cb
        conf['statistics.interval.ms'] = T

    consume_workload(conf, topics)
