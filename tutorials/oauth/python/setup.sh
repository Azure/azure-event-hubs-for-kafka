#!/bin/bash

sudo apt-get update

sudo apt-get install python3.8

echo "Setting up required dependencies"
sudo pip3 --disable-pip-version-check --no-cache-dir install -r
echo "Try running the samples now!"
