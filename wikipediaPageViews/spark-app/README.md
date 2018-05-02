## Prerequisites
- [Java](https://java.com/en/download/)
- [Gradle](https://gradle.org/)
- [Scala](https://www.scala-lang.org/)

### Build
`./gradlew clean build`


## What the application does?
Take a look at *src->main->scala->template->spark* directory

We have two Items here. 

The trait `InitSpark` which is extended by any class that wants to run spark code. This trait has all the code for initialization. I have also supressed the logging to only error levels for less noise.

The file `Main.scala` has the executable class `Main`. 
In this class, I do few things

- Read command line args [date:time] separated by space, such as: '2018-04-30:01 2018-04-30:02' and set calendarinstance (defaults to current time) and iterate over the requested queries
- Check if the results are already present on disk for the query requested if so bail out and save some time, Assuming we arent moving the results off to remote filesystem
- Download the pageview files into an rdd assuming we are always going to URL: https://dumps.wikimedia.org/other/pageviews/. 
- Read a blacklist file and setup a bloom filter based on page titles
- Sort the rdd read of the downloaded file in descending order of views 
- Transform the sorted rdd to data frame apply bloom filter configured above and save the limit'ed results of top 25 to .csv file named with date-hour extension
- Delete the PageViews file from local disk to save some space 

```
## Libraries Included
- Spark - 2.1.1

## Useful Links
- [Spark Docs - Root Page](http://spark.apache.org/docs/latest/)
- [Spark Programming Guide](http://spark.apache.org/docs/latest/programming-guide.html)
- [Spark Latest API docs](http://spark.apache.org/docs/latest/api/)
- [Scala API Docs](http://www.scala-lang.org/api/2.12.1/scala/)
