#!/bin/bash

db_instance="db"

echo "Taking down $db_instance"
docker-compose stop $db_instance

sleep_time_1=60
echo "Sleeping for $sleep_time_1 seconds before bringing up db instance"
sleep $sleep_time_1

echo "Bringing up $db_instance"
docker start $db_instance
