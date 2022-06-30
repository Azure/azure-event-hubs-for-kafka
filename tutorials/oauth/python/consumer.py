#!/usr/bin/env python
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Copyright 2016 Confluent Inc.
# Licensed under the MIT License.
# Licensed under the Apache License, Version 2.0
#
# Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

import signal
import sys
import time
from confluent_kafka import Consumer
from azure.identity import DefaultAzureCredential
from dotenv import load_dotenv
import os

load_dotenv()

FULLY_QUALIFIED_NAMESPACE= os.environ['EVENT_HUB_HOSTNAME']
EVENTHUB_NAME = os.environ['EVENT_HUB_NAME']
CONSUMER_GROUP='$Default'
AUTH_SCOPE= "https://" + FULLY_QUALIFIED_NAMESPACE +"/.default"

# AAD
cred = DefaultAzureCredential()


def _get_token(config):
    """Note here value of config comes from sasl.oauthbearer.config below.
    It is not used in this example but you can put arbitrary values to
    configure how you can get the token (e.g. which token URL to use)
    """
    access_token = cred.get_token(AUTH_SCOPE)
    return access_token.token, time.time() + access_token.expires_on


consumer = Consumer({
    "bootstrap.servers": FULLY_QUALIFIED_NAMESPACE + ":9093",
    "sasl.mechanism": "OAUTHBEARER",
    "security.protocol": "SASL_SSL",
    "oauth_cb": _get_token,
    "group.id": CONSUMER_GROUP,
    # "debug": "broker,topic,msg"
})


def signal_handler(sig, frame):
    print("exiting")
    consumer.close()
    sys.exit(0)


signal.signal(signal.SIGINT, signal_handler)

print("consuming " + EVENTHUB_NAME)
consumer.subscribe([EVENTHUB_NAME])

while True:
    msg = consumer.poll(1.0)

    if msg is None:
        continue
    if msg.error():
        print(f"Consumer error: {msg.error()}")
        continue

    print(
        f"Received message [{msg.partition()}]: {msg.value().decode('utf-8')}")
