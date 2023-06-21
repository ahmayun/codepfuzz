package fuzzer


trait ExecutableProgram {
  def invokeMain(args: Array[String]): Any
  def name: String
  def classname: String
  def classpath: String
  def args: Array[String]

}

class Program(val name: String,
              val classname: String,
              val classpath: String,
              val main: Array[String] => Unit,
              val args: Array[String]) extends ExecutableProgram {
  def invokeMain(args: Array[String]): Unit = {
    main(args)
  }
}

class DynLoadedProgram( val name: String,
                        val classname: String,
                        val classpath: String,
                        val args: Array[String]) extends ExecutableProgram {

  def invokeMain(_args: Array[String]): ProvInfo = {
    val some = utils.reflection.DynamicClassLoader.invokeMethod(classname, "main", _args)
    println(some)
    val Some(coDepInfo) = some
    coDepInfo.asInstanceOf[ProvInfo]
  }
}

// Can add this as an overloaded constructor because scala complains
class InstrumentedProgram(val name: String,
                          val classname: String,
                          val classpath: String,
                          val main: Array[String] => ProvInfo,
                          val args: Array[String]) extends ExecutableProgram {
  def invokeMain(args: Array[String]): ProvInfo = {
    main(args)
  }
}

class ExecStats(
                 val stdout: String,
                 val stderr: String,
                 val input: Array[String],
                 val crashed: Boolean
               ) {

}
