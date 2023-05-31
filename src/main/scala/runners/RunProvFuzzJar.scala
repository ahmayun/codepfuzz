package runners

import fuzzer.{FuzzStats, Fuzzer, Global, InstrumentedProgram, NewFuzzer, Program}
import guidance.{BigFuzzGuidance, ProvFuzzGuidance, ProvFuzzRSGuidance}
import scoverage.report.ScoverageHtmlWriter
import scoverage.{IOUtils, Serializer}
import utils.ProvFuzzUtils

import java.io.File

object RunProvFuzzJar {

  def main(args: Array[String]): Unit = {

    val (benchmarkName, duration, outDir, inputFiles) = if (!args.isEmpty) {
      (args(0), args(1), args(2), args.takeRight(args.length-3))
    } else {
      val name = "WebpageSegmentation"
      val Some(files) = Config.mapInputFilesReduced.get(name)
      (name, "20", s"target/codepfuzz-output/$name", files)
    }

    val Some(funFuzzable) = Config.mapFunFuzzables.get(benchmarkName)
    val benchmarkClass = s"examples.faulty.$benchmarkName"
//    val Some(funProbeAble) = Config.mapFunProbeAble.get(benchmarkName)
    val Some(provInfo) = Config.provInfos.get(benchmarkName)
    // ========================================================

    val benchmarkPath = s"src/main/scala/${benchmarkClass.split('.').mkString("/")}.scala"
    val program = new Program(
      benchmarkName,
      benchmarkClass,
      benchmarkPath,
      funFuzzable,
      inputFiles)

    val guidance = new ProvFuzzGuidance(inputFiles, provInfo, duration.toInt)

    val (stats, timeStartFuzz, timeEndFuzz) = NewFuzzer.FuzzMutants(program, program, guidance, outDir)

    reportStats(program, stats, timeStartFuzz, timeEndFuzz)

    println("ProvInfo: ")
    println(provInfo)
  }



  def reportStats(program: Program, stats: FuzzStats, timeStartFuzz: Long, timeEndFuzz: Long): Unit = {
    val durationProbe = 0.1f // (timeEndProbe - timeStartProbe) / 1000.0
    val durationFuzz = (timeEndFuzz - timeStartFuzz) / 1000.0
    val durationTotal = durationProbe + durationFuzz

    // Printing results
    stats.failureMap.foreach { case (msg, (_, c, i)) => println(s"i=$i:line=${getLineNo(program.name, msg.mkString(","))} $c x $msg") }
    stats.failureMap.foreach { case (msg, (_, c, i)) => println(s"i=$i:line=${getLineNo(program.name, msg.mkString(","))} x $c") }
    stats.failureMap.map { case (msg, (_, c, i)) => (getLineNo(program.name, msg.mkString("\n")), c, i) }
      .groupBy(_._1)
      .map { case (line, list) => (line, list.size) }
      .toList.sortBy(_._1)
      .foreach(println)

    println(s"=== RESULTS: ProvFuzz ${program.name} ===")
    println(s"failures: ${stats.failureMap.map { case (_, (_, _, i)) => i + 1 }.toSeq.sortBy(i => i).mkString(",")}")
    println(s"# of Failures: ${stats.failures} (${stats.failureMap.keySet.size} unique)")
    println(s"coverage progress: ${stats.plotData._2.map(limitDP(_, 2)).mkString(",")}")
    println(s"iterations: ${Global.iteration}")
    println(s"Total Time (s): ${limitDP(durationTotal, 2)} (P: $durationProbe | F: $durationFuzz)")
  }

  def limitDP(d: Double, dp: Int): Double = {
    BigDecimal(d).setScale(dp, BigDecimal.RoundingMode.HALF_UP).toDouble
  }

  def getLineNo(filename: String, trace: String): String = {
    val pattern = s"""$filename.scala:(\\d+)"""
    pattern.r.findFirstIn(trace) match {
      case Some(str) => str.split(':').last
      case _ => "-"
    }
  }
}
