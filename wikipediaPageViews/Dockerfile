FROM debian:jessie

# Environment
ENV SPARK_VERSION 2.1.1
ENV HADOOP_VERSION 2.7
ENV SCALA_VERSION 2.11
ENV SPARK_HOME /usr/local/spark

RUN echo "deb http://http.debian.net/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list

RUN apt-get -y update && apt-get install -y wget && apt-get install -y -t jessie-backports --no-install-recommends openjdk-8-jdk

# Spark
RUN cd /tmp && \
    wget http://d3kbcqa49mib13.cloudfront.net/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz && \
    tar -xvzf spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz -C /usr/local && \
    rm spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz

RUN cd /usr/local && ln -s spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} spark

#Install Cron
RUN apt-get update
RUN apt-get -y install cron

# Add crontab file in the cron directory
ADD crontab /etc/cron.d/spark-cron

# Give execution rights on the cron job
RUN chmod 0644 /etc/cron.d/spark-cron

# Create the log file to be able to run tail
#RUN touch /var/log/spark.log

#RUN /etc/init.d/cron start 

#RUN /usr/bin/crontab /etc/cron.d/spark-cron

# Run the command on container startup
#CMD cron && tail -f /var/log/spark.log

EXPOSE 8080
EXPOSE 6066
EXPOSE 7077
EXPOSE 4040
