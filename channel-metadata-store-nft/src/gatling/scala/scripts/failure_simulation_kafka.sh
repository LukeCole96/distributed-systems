#!/bin/bash

kafka_brokers=("kafka-1" "kafka-2" "kafka-3")

random_kafka_broker=${kafka_brokers[$RANDOM % ${#kafka_brokers[@]}]}

echo "Taking down $random_kafka_broker"
docker-compose stop $random_kafka_broker

sleep_time=$(( (RANDOM % 30) + 10 ))
echo "Sleeping for $sleep_time seconds"
sleep $sleep_time

echo "Bringing up $random_kafka_broker"
docker-compose start $random_kafka_broker
