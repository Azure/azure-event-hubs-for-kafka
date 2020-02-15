#!/bin/bash
echo "Downloading/installing necessary dependencies"
sudo apt-get install git openssl libssl-dev build-essential python-pip python-dev librdkafka-dev
git clone https://github.com/edenhill/librdkafka

echo "Setting up librdkafka"
cd librdkafka
./configure
make
sudo make install
cd ..

echo "Setting up Confluent's Python Kafka library"
sudo pip install confluent-kafka
echo "Try running the samples now!"
