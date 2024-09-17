#!/bin/bash

sleep 30
hazelcast_instances=($(docker ps --filter "name=hazelcast" --format "{{.Names}}"))

instance_1=${hazelcast_instances[$RANDOM % ${#hazelcast_instances[@]}]}
instance_2=$instance_1
while [ "$instance_2" == "$instance_1" ]; do
  instance_2=${hazelcast_instances[$RANDOM % ${#hazelcast_instances[@]}]}
done

echo "Taking down $instance_1"
docker stop $instance_1

sleep_time_1=$(( (RANDOM % 30) + 10 ))
echo "Sleeping for $sleep_time_1 seconds before taking down the second instance"
sleep $sleep_time_1

echo "Taking down $instance_2"
docker stop $instance_2

sleep_time_2=$(( (RANDOM % 30) + 10 ))
echo "Sleeping for $sleep_time_2 seconds before bringing the instances back up"
sleep $sleep_time_2

echo "Bringing up $instance_1"
docker start $instance_1

echo "Bringing up $instance_2"
docker start $instance_2