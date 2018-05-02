# Spark app for deployment 


## Technologies
- Java 8
- Scala 2.11
- Gradle
- Spark
- Docker

## How to run manually
Build the application:
```
./gradlew clean build
```

```
docker build -t spark:2.1.1 .
```

```
mkdir out
```

```
docker-compose run -d spark
```

open Spark container with command:
```
docker exec -it $(docker-compose ps -q spark) bash
```


Start master and worker in spark container:
```
#Run the following to set MASTER variable
MASTER="spark://"$(tail -1 /etc/hosts | awk '{print $2}')":7077"
/usr/local/spark/sbin/start-master.sh
/usr/local/spark/sbin/start-slave.sh $MASTER
```

Please note that app/ and script/ and out/ directories are mapped into the docker container to store application jar, blacklist|sparkjobs.sh, filtered output/results respectively.
Run spark application in spark container:
```
/usr/local/spark/bin/spark-submit \
--master $MASTER \
app/spark-app-1.0-SNAPSHOT-all.jar 
```

Run spark application in spark container with specific range :
```
/usr/local/spark/bin/spark-submit \
--master $MASTER \
app/spark-app-1.0-SNAPSHOT-all.jar 2018-04-30:01 2018-04-30:02
```

Check the results in out directory within container or on host 
cat 2018-04-30-01results.csv/part-00000-15545317-3018-4a1a-acae-90db7e7c0675.csv 

Optional 1: If you want delete locked build's directories, then run:
```
sudo rm -rf spark-app/build 
```

Optional 2: If you want close the docker container, then run:
```
docker-compose down 
```

## License
The MIT License (MIT)
