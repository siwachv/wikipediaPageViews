#!/usr/bin/env bash

docker-compose down
./gradlew clean && \
./gradlew shadowJar

docker-compose up -d

docker exec -dit $(docker-compose ps -q spark) bash ./usr/local/spark/sbin/start-master.sh -i $SPARK_LOCAL_IP
docker exec -dit $(docker-compose ps -q spark) bash ./usr/local/spark/sbin/start-slave.sh $MASTER
