#!/usr/bin/env python
#
# Copyright (c) Microsoft Corporation. All rights reserved.
# Copyright 2023 Azure Inc.
# Licensed under the MIT License.
# Licensed under the Apache License, Version 2.0
#

from azure.identity import DefaultAzureCredential
from functools import partial
import os
import requests
import time


TENANT_ID = os.environ.get('AZURE_TENANT_ID')
CLIENT_ID = os.environ.get('AZURE_CLIENT_ID')
CLIENT_SECRET = os.environ.get('AZURE_CLIENT_SECRET')


def get_oauth_config(namespace):
    conf = {
        'bootstrap.servers': '%s:9093' % namespace,

        # Required OAuth2 configuration properties
        'security.protocol': 'SASL_SSL',
        'sasl.mechanism': 'OAUTHBEARER'
    }
    return conf


def get_azure_config(namespace):
    def oauth_cb(cred, namespace_fqdn, config):
        # confluent_kafka requires an oauth callback function to return (str, float) with the values of (<access token>, <expiration date in seconds from epoch>)

        # cred: an Azure identity library credential object. Ex: an instance of DefaultAzureCredential, ManagedIdentityCredential, etc
        # namespace_fqdn: the FQDN for the target Event Hubs namespace. Ex: 'mynamespace.servicebus.windows.net'
        # config: confluent_kafka passes the value of 'sasl.oauthbearer.config' as the config param

        access_token = cred.get_token('https://%s/.default' % namespace_fqdn)
        return access_token.token, access_token.expires_on

    # Azure credential
    # See https://docs.microsoft.com/en-us/azure/developer/python/sdk/authentication-overview
    cred = DefaultAzureCredential()

    # Producer configuration
    # See https://github.com/edenhill/librdkafka/blob/master/CONFIGURATION.md
    conf = get_oauth_config(namespace)

    # the resulting oauth_cb must accept a single `config` parameter, so we use partial to bind the namespace/identity to our function
    conf['oauth_cb'] = partial(oauth_cb, cred, namespace)
    return conf


# Using Kafka oauthbearer OIDC semantics that decodes JWT tokens and uses exp claim
# KIP-768: Extend SASL/OAUTHBEARER with Support for OIDC
#
def get_oidc_config(namespace):
    conf = get_oauth_config(namespace)
    conf.update({
        'sasl.oauthbearer.method': 'oidc',
        'sasl.oauthbearer.client.id': CLIENT_ID,
        'sasl.oauthbearer.client.secret': CLIENT_SECRET,
        'sasl.oauthbearer.token.endpoint.url': 'https://login.microsoftonline.com/%s/oauth2/v2.0/token' % TENANT_ID,
        'sasl.oauthbearer.scope': 'https://%s/.default' % namespace,
    })

    return conf


# Using expires_in field from the token response to treat OAUTHBEARER as opaque
# and avoid decoding JWT by utilizing RFC9068, RFC6749 concepts.
# https://datatracker.ietf.org/doc/html/rfc9068#name-privacy-considerations
# https://www.rfc-editor.org/rfc/rfc6749#section-4.2.2
#
def get_opaque_config(namespace):
    def oauth_cb(config):
        # take the time before request first
        token_exp_time = int(time.time())

        token_resp = requests.post(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token" % TENANT_ID,
            auth=(CLIENT_ID, CLIENT_SECRET),
            data={
                'grant_type': 'client_credentials',
                'scope': 'https://%s/.default' % namespace
            }
        )

        token_resp = token_resp.json()

        # add expires_in value which is a token validity time
        # in seconds from the time the response was generated
        #
        token_exp_time += int(token_resp['expires_in'])

        return token_resp['access_token'], token_exp_time

    conf = get_oauth_config(namespace)
    conf['oauth_cb'] = oauth_cb

    return conf



# Returns producer configs for azure, opaque, oidc modes
#
def get_config(namespace, mode):
    if mode == 'azure':
        conf = get_azure_config(namespace)
    elif mode == 'oidc':
        conf = get_oidc_config(namespace)
    elif mode == 'opaque':
        conf = get_opaque_config(namespace)

    return conf