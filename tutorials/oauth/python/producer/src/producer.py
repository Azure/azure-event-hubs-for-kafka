import time
import json 
import random 
from datetime import datetime

from data_generator import generate_message
from token_provider import AzureTokenProvider
from kafka import KafkaProducer

# Messages will be serialized as JSON 
def serializer(message):
    return json.dumps(message).encode('utf-8')

# Read config for connection
with open("../config/oauth_config.json") as file:
    config = json.load(file)

# Kafka Producer
producer = KafkaProducer(
        bootstrap_servers=[config["bootstrap_servers"]],
        security_protocol=config["security_protocol"],
        sasl_mechanism=config["sasl_mechanism"],
        sasl_oauth_token_provider=AzureTokenProvider(token_scope=config["token_scope"]),
        value_serializer=serializer
    )

# Event Hub/Kafka topic name. In Kafka terminology, an Event Hub corresponds to a topic.
topic = 'example_topic'

# CODE IS INSPIRED BY: https://betterdatascience.com/apache-kafka-in-python-how-to-stream-data-with-producers-and-consumers/
# CREDITS TO: Dario Radečić
if __name__ == '__main__':
    # Infinite loop - runs until you kill the program
    while True:
        # Generate a message
        dummy_message = generate_message()
        
        # Send it to our 'messages' topic
        print(f'Producing message @ {datetime.now()} | Message = {str(dummy_message)}')
        producer.send(topic, dummy_message)
        print('Message sent to Event Hub.')

        # Sleep for a random number of seconds
        time_to_sleep = random.randint(1, 11)
        time.sleep(time_to_sleep)