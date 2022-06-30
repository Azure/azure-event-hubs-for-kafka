#!/usr/bin/env python
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Copyright 2016 Confluent Inc.
# Licensed under the MIT License.
# Licensed under the Apache License, Version 2.0
#
# Original Confluent sample modified for use with Azure Event Hubs for Apache Kafka Ecosystems

import time
from confluent_kafka import Producer
from azure.identity import DefaultAzureCredential
from dotenv import load_dotenv
import os

load_dotenv()

FULLY_QUALIFIED_NAMESPACE= os.environ['EVENT_HUB_HOSTNAME']
EVENTHUB_NAME = os.environ['EVENT_HUB_NAME']
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


producer = Producer({
    "bootstrap.servers": FULLY_QUALIFIED_NAMESPACE + ":9093",
    "sasl.mechanism": "OAUTHBEARER",
    "security.protocol": "SASL_SSL",
    "oauth_cb": _get_token,
    "enable.idempotence": True,
    "acks": "all",
    # "debug": "broker,topic,msg"
})


def delivery_report(err, msg):
    """Called once for each message produced to indicate delivery result.
    Triggered by poll() or flush()."""
    if err is not None:
        print(f"Message delivery failed: {err}")
    else:
        print(f"Message delivered to {msg.topic()} [{msg.partition()}]")


some_data_source = [str(i) for i in range(1000)]
for data in some_data_source:
    # Trigger any available delivery report callbacks from previous produce() calls
    producer.poll(0)

    # Asynchronously produce a message, the delivery report callback
    # will be triggered from poll() above, or flush() below, when the message has
    # been successfully delivered or failed permanently.
    producer.produce(EVENTHUB_NAME, f"Hello {data}".encode("utf-8"), callback=delivery_report)

# Wait for any outstanding messages to be delivered and delivery report
# callbacks to be triggered.
producer.flush()
