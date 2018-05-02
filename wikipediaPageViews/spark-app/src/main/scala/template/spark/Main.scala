package template.spark
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.spark.sql.functions._
import sys.process._
import java.net.URL
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import scala.util.Try
import java.sql.Date

import org.apache.spark.sql.functions._

case class PageCounts(project: String, pageTitle: String, views: Long)
case class FilterCounts(project: String, pageTitle: String)

/**
  * Object Main
  *
  * Object reads files from URL: https://dumps.wikimedia.org/other/pageviews/.
  * and then filters top 25 based on blacklist file [stored in /scripts folder (shared with container)]
  *
  * For starting this application you need to run steps manually as detailed in README-deployment
  *
  * @author Vikram Siwach
  * @since 0.0.1
  **/

object Main extends InitSpark {
def main(args: Array[String]): Unit = {
import spark.implicits._

var now = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
var year = now.get(Calendar.YEAR)
var month = now.get(Calendar.MONTH) + 1
var day = now.get(Calendar.DAY_OF_MONTH)
var hour = now.get(Calendar.HOUR_OF_DAY)
var range: Array[String] =  Array("%04d-%02d-%02d:%02d".format(year,month,day,hour)) //default to current if args not supplied

 /**
    * @param args consists of:
    *             - line of [date:time] separated by space, such as: '2018-04-30:01 2018-04-30:02'
    *  trusting user knows what he is doing so not validating the arguments ....
    */

    if (args.length != 0)
        range = args

	for(i <- 0 until range.length) { //iterating over the argument range

	//stripping date:time from args
	val datetime = range(i).split(":").map(_.trim)
	val date = Date.valueOf(datetime(0))
	hour = datetime(1).toInt

	now.setTime(date) //overwriting now, not happy with it :(
	year = now.get(Calendar.YEAR)
	month = now.get(Calendar.MONTH) + 1
	day = now.get(Calendar.DAY_OF_MONTH) + 1

    val outDir = "/out/";
    val conf = sc.hadoopConfiguration
    val fs = org.apache.hadoop.fs.FileSystem.get(conf)
    val exists = fs.exists(new org.apache.hadoop.fs.Path(outDir + "%02d-%02d-%02d-%02d".format(year,month,day,hour) + "results.csv"))

    if (!exists){

      val url_root = "https://dumps.wikimedia.org/other/pageviews"
      val url_to_download = url_root +  "/%d/%d-%02d/pageviews-%d%02d%02d-%02d0000.gz".format(year, year, month, year, month, day, hour)
      val local_file = outDir + "pageviews-%d%02d%02d-%02d0000.gz".format(year, month, day, hour)

      println("Executing Wikipedia Pageview Job for files:" + url_to_download)

      new URL(url_to_download) #> new File(local_file) !! // super bad http connection handler doesnt have a retry mechanism so bound to fail if wikipedia doesnt update stats in time...

      val pageviews = sc.textFile(local_file)
      val blacklist = sc.textFile("/script/blacklist_domains_and_pages")

      val entries = {
        blacklist.map(_.split(" ")).map(l => FilterCounts(
          Try(l(0)) getOrElse("notFound"), Try(StringEscapeUtils.unescapeHtml4(l(1))) getOrElse("notFound")))
      }

      val filter = spark.createDataFrame(entries).toDF("project", "pageTitle").drop("project") //dropping project columns as seems to add minimum value for domain search

      def might_contain(f: org.apache.spark.util.sketch.BloomFilter) = udf((x: String) =>
        if(x != null) f.mightContainString(x) else false)

      val expectedNumItems: Long = 1000000
      val fpp: Double = 0.005

      // creating a bloom filter for black list
      val sbf = filter.stat.bloomFilter($"pageTitle", expectedNumItems, fpp)

      val record = {
        pageviews.map(_.split(" ")).map(l =>PageCounts(
          Try(l(0)) getOrElse("notFound"), Try(StringEscapeUtils.unescapeHtml4(l(1))) getOrElse("notFound"), l(2).toLong)) // should write exception handler for last column but being lazy ...
      }

      val sort = record.sortBy { case PageCounts(project, pageTitle, views ) => (-views, pageTitle, project) }

      val result = spark.createDataFrame(sort).toDF("project", "pageTitle", "views")

      result.where(!might_contain(sbf)($"pageTitle")).limit(25).write.format("com.databricks.spark.csv").option("delimiter", " ").save(outDir + "%02d-%02d-%02d-%02d".format(year,month,day,hour) + "results.csv")

	//deleting the pageview files from local disk
	fs.deleteOnExit(new org.apache.hadoop.fs.Path(local_file))
    }
    else {
      println("Already been executed skipping check :" + outDir + "%02d-%02d-%02d-%02d".format(year,month,day,hour) + "results.csv")
    } //already processsed the results for the specific day:hour
  }
}
}
