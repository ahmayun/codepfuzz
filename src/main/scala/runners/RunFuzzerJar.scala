package runners

import fuzzer.{DynLoadedProgram, ExecutableProgram, FuzzStats, Global, InstrumentedProgram, NewFuzzer, Program, ProvInfo}
import guidance.ProvFuzzGuidance
import org.apache.spark.{SparkConf, SparkContext}
import transformers.SparkProgramTransformer


object RunFuzzerJar {

  def main(args: Array[String]): Unit = {

    val (benchmarkName, duration, outDir, inputFiles) = if (!args.isEmpty) {
      (args(0), args(1), args(2), args.takeRight(args.length-3))
    } else {
      val name = "WebpageSegmentation"
      val Some(files) = Config.mapInputFilesReduced.get(name)
      (name, "20", s"target/depfuzz-output/$name", files)
    }
    val Some(funFuzzable) = Config.mapFunFuzzables.get(benchmarkName)
//    val Some(codepInfo) = Config.provInfos.get(benchmarkName)
    val outPathInstrumented = "src/main/scala/examples/instrumented"
    val outPathFWA = "src/main/scala/examples/fwa"

    val sparkProgramClass = s"examples.benchmarks.$benchmarkName"
    val sparkProgramPath = s"src/main/scala/${sparkProgramClass.split('.').mkString("/")}.scala"

    val instPackage = "examples.instrumented"
    val instProgramClass = s"$instPackage.$benchmarkName"
    val instProgramPath = s"$outPathInstrumented/$benchmarkName.scala"
    val fwaPackage = "examples.fwa"
    val fwaProgramClass = s"$fwaPackage.$benchmarkName"
    val fwaProgramPath = s"$outPathFWA/$benchmarkName.scala"
//
//    val transformer = new SparkProgramTransformer(sparkProgramPath)
//
//    transformer
//      .changePackageTo(instPackage)
//      .enableTaintProp()
//      .attachMonitors()
//      .writeTo(instProgramPath)
//
//    transformer
//      .changePackageTo(fwaPackage)
//      .replaceImports(
//        Map(
//          "org.apache.spark.SparkConf" -> "abstraction.SparkConf",
//          "org.apache.spark.SparkContext" -> "abstraction.SparkContext"
//        )
//      )
//      .writeTo(fwaProgramPath)

    val sc = new SparkContext(
      new SparkConf()
        .setAppName("DepFuzz")
        .setMaster("local[*]")
    )

    val instProgram = new DynLoadedProgram[ProvInfo](
      benchmarkName,
      instProgramClass,
      instProgramPath,
      inputFiles,
      {
        case Some(coDepInfo) => coDepInfo.asInstanceOf[ProvInfo]
        case _ => null
      }
    )

    val codepInfo = instProgram.invokeMain(inputFiles:+"local[*]")

    val program = new DynLoadedProgram[Unit](
      benchmarkName,
      fwaProgramClass,
      fwaProgramPath,
//      funFuzzable,
      inputFiles,
      {case _ => Unit}
    )

    val guidance = new ProvFuzzGuidance(inputFiles, codepInfo, duration.toInt)
    val (stats, timeStartFuzz, timeEndFuzz) = NewFuzzer.FuzzMutants(program, program, guidance, outDir, false)
    reportStats(program, stats, timeStartFuzz, timeEndFuzz)
    println("Co-dependence Info: ")
    println(codepInfo)
  }

  def reportStats(program: ExecutableProgram, stats: FuzzStats, timeStartFuzz: Long, timeEndFuzz: Long): Unit = {
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
