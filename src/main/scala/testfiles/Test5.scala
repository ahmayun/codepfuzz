package testfiles

import org.apache.spark.{SparkConf, SparkContext}

object Test5 {

  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(
      new SparkConf()
        .setAppName("DepFuzz")
        .setMaster("local[*]")
    )
    examples.instrumented.WebpageSegmentation.main(Array("seeds/reduced_data/webpage_segmentation/before", "seeds/reduced_data/webpage_segmentation/after", "local[*]"))
  }
}
