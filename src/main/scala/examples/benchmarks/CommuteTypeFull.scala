package examples.benchmarks

import org.apache.spark.{SparkConf, SparkContext}

object CommuteTypeFull extends Serializable {

  def main(args: Array[String]) {
    val conf = new SparkConf().setMaster(if(args.length > 2) args(2) else "local[*]")
    conf.setAppName("CommuteTime")
    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")

    val trips = sc.textFile(args(0)).map(s => s.split(","))
      .map { cols =>
          (cols(1), Integer.parseInt(cols(3)) / Integer.parseInt(cols(4)))
        }
    val locations = sc.textFile(args(1)).map(s => s.filter(_ != '\"').split(","))
      .map { cols =>
        (cols(0), cols(3))
      }
      .filter {
        s =>
          s._2.equals("Los Angeles")
      }

    val joined = trips.join(locations)
    joined
      .map { s =>
        // Checking if speed is < 25mi/hr
        val speed = s._2._1
        if (speed > 40) {
          ("car", speed)
        } else if (speed > 15) {
          ("public", speed)
        } else {
          ("onfoot", speed)
        }
      }
      .reduceByKey {
        case (a, b) =>
          a + b
      }
      .collect
      .foreach(println)
  }

}