/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
//package org.apache.spark.examples

import org.apache.spark.{SparkConf, SparkContext}
//import com.example.hello._
import org.apache.spark.SparkContext._

/**
  * Executes a roll up-style query against Apache logs.
  *
  * This is adapted from Apache Spark GitHub: https://github.com/apache/spark/blob/master/examples/src/main/scala/org/apache/spark/examples/LogQuery.scala
  */
object LogQuery {
  val exampleApacheLogs = List(
    """10.10.10.10 - "FRED" [18/Jan/2013:17:56:07 +1100] "GET http://images.com/2013/Generic.jpg
      | HTTP/1.1" 304 315 "http://referall.com/" "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1;
      | GTB7.4; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.04506.648; .NET CLR
      | 3.5.21022; .NET CLR 3.0.4506.2152; .NET CLR 1.0.3705; .NET CLR 1.1.4322; .NET CLR
      | 3.5.30729; Release=ARP)" "UD-1" - "image/jpeg" "whatever" 0.350 "-" - "" 265 923 934 ""
      | 62.24.11.25 images.com 1358492167 - Whatup""".stripMargin.lines.mkString,
    """10.10.10.10 - "FRED" [18/Jan/2013:18:02:37 +1100] "GET http://images.com/2013/Generic.jpg
      | HTTP/1.1" 304 306 "http:/referall.com" "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1;
      | GTB7.4; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.04506.648; .NET CLR
      | 3.5.21022; .NET CLR 3.0.4506.2152; .NET CLR 1.0.3705; .NET CLR 1.1.4322; .NET CLR
      | 3.5.30729; Release=ARP)" "UD-1" - "image/jpeg" "whatever" 0.352 "-" - "" 256 977 988 ""
      | 0 73.23.2.15 images.com 1358492557 - Whatup""".stripMargin.lines.mkString
  )

  def main(args: Array[String]) {
    //var sparkconf2 = new SparkConf().setAppName() sparkConf().setAppName("Log 1");
    //val sc=new sparkcontext()

    val sparkconf = new SparkConf().setAppName("Log Query").setMaster("local[2]")

    val sc = new SparkContext(sparkconf)

    val dataSet = sc.parallelize(exampleApacheLogs)
    // scalastyle:off
    val apacheLogRegex =
      """^([\d.]+) (\S+) (\S+) \[([\w\d:/]+\s[+\-]\d{4})\] "(.+?)" (\d{3}) ([\d\-]+) "([^"]+)" "([^"]+)".*""".r
    // scalastyle:on
    /** Tracks the total query count and number of aggregate bytes for a particular group. */
    class Stats(val count: Int, val numBytes: Int) extends Serializable {
      def merge(other: Stats): Stats = new Stats(count + other.count, numBytes + other.numBytes)
      override def toString: String = "bytes=%s\tn=%s".format(numBytes, count)
    }

    def extractKey(line: String): (String, String, String) = {
      apacheLogRegex.findFirstIn(line) match {
        case Some(apacheLogRegex(ip, _, user, dateTime, query, status, bytes, referer, ua)) =>
          if (user != "\"-\"") (ip, user, query)
          else (null, null, null)
        case _ => (null, null, null)
      }
    }

    def extractStats(line: String): Stats = {
      apacheLogRegex.findFirstIn(line) match {
        case Some(apacheLogRegex(ip, _, user, dateTime, query, status, bytes, referer, ua)) =>
          new Stats(1, bytes.toInt)
        case _ => new Stats(1, 0)
      }
    }

    //println(Point(1,2))
    //testcall();
    dataSet.map(line => (extractKey(line), extractStats(line)))
      .reduceByKey((a, b) => a.merge(b))
      .collect().foreach{
      case (user, query) => println("%s\t%s".format(user, query))}

    sc.stop()
  }
}
