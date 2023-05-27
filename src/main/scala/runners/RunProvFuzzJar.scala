package runners

import fuzzer.{Fuzzer, Global, InstrumentedProgram, Program}
import guidance.{BigFuzzGuidance, ProvFuzzGuidance, ProvFuzzRSGuidance}
import scoverage.report.ScoverageHtmlWriter
import scoverage.{IOUtils, Serializer}
import utils.ProvFuzzUtils

import java.io.File

object RunProvFuzzJar {

  def main(args: Array[String]): Unit = {

    println("RunProvFuzzJar called with following args:")
    println(args.mkString("\n"))

    // ==P.U.T. dependent configurations=======================
    val benchmarkName = args(0)
    val mode = args(1)
    val duration = args(2)
    val outDir = args(3)

    if(!Array("rs","cm","full").contains(mode)) throw new IllegalArgumentException()

    val Some(inputFiles) = if (!mode.equals("cm"))
      Config.mapInputFilesReduced.get(benchmarkName)
    else
      Config.mapInputFilesWeak.get(benchmarkName)

    val Some(funFuzzable) = Config.mapFunFuzzables.get(benchmarkName)
    val Some(schema) = Config.mapSchemas.get(benchmarkName)
    val benchmarkClass = s"examples.faulty.$benchmarkName"
//    val Some(funProbeAble) = Config.mapFunProbeAble.get(benchmarkName)
    val Some(provInfo) = if(mode.equals("cm")) Config.provInfosForWeak.get(benchmarkName) else Config.provInfos.get(benchmarkName)
    // ========================================================

    val scoverageOutputDir = s"$outDir/scoverage-results"

    val benchmarkPath = s"src/main/scala/${benchmarkClass.split('.').mkString("/")}.scala"
    val program = new Program(
      benchmarkName,
      benchmarkClass,
      benchmarkPath,
      funFuzzable,
      inputFiles)


//    val probeClass = s"examples.monitored.$benchmarkName"
//    val probePath = s"src/main/scala/${probeClass.split('.').mkString("/")}.scala"
//    val probeProgram = new InstrumentedProgram(benchmarkName,
//      probeClass,
//      probePath,
//      funProbeAble,
//      inputFiles)

    // Probing and Fuzzing
//    val (provInfo, timeStartProbe, timeEndProbe) = ProvFuzzUtils.Probe(probeProgram)
    val guidance = if(!mode.equals("rs"))
      new ProvFuzzGuidance(inputFiles, schema, provInfo, duration.toInt)
    else
      new ProvFuzzRSGuidance(inputFiles, schema, duration.toInt)

    println("ProvInfo: ")
    println(provInfo)

//    sys.exit(0)
    val (stats, timeStartFuzz, timeEndFuzz) = Fuzzer.Fuzz(program, guidance, outDir)

    // Finalizing
    val coverage = Serializer.deserialize(new File(s"$scoverageOutputDir/scoverage.coverage"))
    val measurementFiles = IOUtils.findMeasurementFiles(scoverageOutputDir)
    val measurements = IOUtils.invoked(measurementFiles)

    coverage.apply(measurements)
    new ScoverageHtmlWriter(Seq(new File("src/main/scala")), new File(scoverageOutputDir)).write(coverage)

    val durationProbe = 0.1f // (timeEndProbe - timeStartProbe) / 1000.0
    val durationFuzz = (timeEndFuzz - timeStartFuzz) / 1000.0
    val durationTotal = durationProbe + durationFuzz


    // Printing results
    stats.failureMap.foreach { case (msg, (_, c, i)) => println(s"i=$i:line=${getLineNo(benchmarkName, msg.mkString(","))} $c x $msg") }
    stats.failureMap.foreach { case (msg, (_, c, i)) => println(s"i=$i:line=${getLineNo(benchmarkName, msg.mkString(","))} x $c") }
    stats.failureMap.map { case (msg, (_, c, i)) => (getLineNo(benchmarkName, msg.mkString("\n")), c, i) }
      .groupBy(_._1)
      .map { case (line, list) => (line, list.size) }
      .toList.sortBy(_._1)
      .foreach(println)

    println(s"=== RESULTS: ProvFuzz $benchmarkName ===")
    println(s"Failures: ${stats.failures} (${stats.failureMap.keySet.size} unique)")
    println(s"failures: ${stats.failureMap.map { case (_, (_, _, i)) => i + 1 }.toSeq.sortBy(i => i).mkString(",")}")
    println(s"coverage progress: ${stats.plotData._2.map(limitDP(_, 2)).mkString(",")}")
    println(s"iterations: ${stats.plotData._1.mkString(",")}")
    println(s"Coverage: ${limitDP(coverage.statementCoveragePercent, 2)}% (gathered from ${measurementFiles.length} measurement files)")
    println(s"Total Time (s): ${limitDP(durationTotal, 2)} (P: $durationProbe | F: $durationFuzz)")
    println(
      s"Config:\n" +
        s"\tProgram: ${program.name}\n" +
        s"\tMutation Distribution M1-M6: ${guidance.get_mutate_probs.mkString(",")}\n" +
        s"\tActual Application: ${guidance.get_actual_app.mkString(",")}\n" +
        s"\tIterations: ${Global.iteration}"
    )
    println("ProvInfo: ")
    println(provInfo)
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
