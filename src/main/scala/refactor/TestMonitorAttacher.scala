package refactor

import java.io.File
import scala.meta._

object TestMonitorAttacher {

  def main(args: Array[String]): Unit = {
    val outputFolder = "src/main/scala/refactor/testout"
    new File(outputFolder).mkdirs()

    TestCases.testCases.foreach { case (testName, testData) =>
      println("-"*3 + s" $testName " + "-"*10)
      val outputFile = s"$outputFolder/$testName.scala"
      val tree = testData.parse[Source].get
      val transformed = MonitorAttacher.attachMonitors(tree)
      println(tree.structure)
      println(transformed.structure)
      MonitorAttacher.writeTransformed(transformed.toString(), outputFile)
//      Source(
//        List(
//          Pkg(
//            Term.Select(Term.Name("refactor"), Term.Name("testout")),
//            List(
//              Import(
//                List(
//                  Importer(Term.Select(Term.Select(Term.Name("org"), Term.Name("apache")), Term.Name("spark")), List(Importee.Name(Name("SparkConf")), Importee.Name(Name("SparkContext")))))
//              ),
//              Defn.Object(
//                Nil,
//                Term.Name("predicateTest2"),
//                Template(
//                  Nil,
//                  Nil,
//                  Self(Name(""), None),
//                  List(
//                    Defn.Def(
//                      Nil,
//                      Term.Name("main"),
//                      Nil,
//                      List(List(Term.Param(Nil, Term.Name("args"), Some(Type.Apply(Type.Name("Array"), List(Type.Name("String")))), None))),
//                      Some(Type.Name("Unit")),
//                      Term.Block(
//                        List(
//                          Defn.Val(Nil, List(Pat.Var(Term.Name("sparkConf"))), None, Term.Apply(Term.Select(Term.Apply(Term.Select(Term.New(Init(Type.Name("SparkConf"), Name(""), List(List()))), Term.Name("setMaster")), List(Lit.String("local[*]"))), Term.Name("setAppName")), List(Lit.String("Test")))),
//                          Defn.Val(Nil, List(Pat.Var(Term.Name("ctx"))), None, Term.New(Init(Type.Name("SparkContext"), Name(""), List(List(Term.Name("sparkConf")))))),
//                          Defn.Val(Nil, List(Pat.Var(Term.Name("data1"))), None,
//                            Term.Apply(
//                              Term.Select(Term.Apply(Term.Select(Term.Name("ctx"), Term.Name("textFile")), List(Lit.String("dummydata/predicateTest2"))), Term.Name("map")),
//                              List(
//                                Term.Block(List(Term.Function(List(Term.Param(Nil, Term.Name("arr"), None, None)), Term.Apply(Term.Select(Term.Name("arr"), Term.Name("split")), List(Lit.String(","))))))
//                              )
//                            )
//                          ),
//                          Defn.Val(Nil, List(Pat.Var(Term.Name("data2"))), None, Term.Apply(Term.Select(Term.Apply(Term.Select(Term.Name("ctx"), Term.Name("textFile")), List(Lit.String("dummydata/predicateTest2"))), Term.Name("map")), List(Term.Block(List(Term.Function(List(Term.Param(Nil, Term.Name("arr"), None, None)), Term.Apply(Term.Select(Term.Name("arr"), Term.Name("split")), List(Lit.String(","))))))))),
//                          Defn.Val(Nil, List(Pat.Var(Term.Name("d1"))), None, Term.Apply(Term.Select(Term.Name("data1"), Term.Name("map")), List(Term.Function(List(Term.Param(Nil, Term.Name("arr"), None, None)), Term.Tuple(List(Term.Select(Term.Name("arr"), Term.Name("head")), Term.Select(Term.Name("arr"), Term.Name("tail")))))))),
//                          Defn.Val(Nil, List(Pat.Var(Term.Name("d2"))), None, Term.Apply(Term.Select(Term.Name("data2"), Term.Name("map")), List(Term.Function(List(Term.Param(Nil, Term.Name("arr"), None, None)), Term.Tuple(List(Term.Select(Term.Name("arr"), Term.Name("head")), Term.Select(Term.Name("arr"), Term.Name("tail")))))))),
//                          Term.Apply(
//                            Term.Select(
//                              Term.Apply(
//                                Term.Select(
//                                  Term.Apply(
//                                    Term.Select(
//                                      Term.Name("d1"), Term.Name("join")
//                                    ),
//                                    List(Term.Name("d2"))
//                                  ), Term.Name("filter")
//                                ), List(Term.PartialFunction(List(Case(Pat.Tuple(List(Pat.Var(Term.Name("k")), Pat.Tuple(List(Pat.Wildcard(), Pat.Wildcard())))), None, Term.ApplyInfix(Term.ApplyInfix(Term.Select(Term.Name("k"), Term.Name("toInt")), Term.Name(">"), Nil, List(Lit.Int(3))), Term.Name("&&"), Nil, List(Term.ApplyInfix(Term.Select(Term.Name("k"), Term.Name("toInt")), Term.Name("<"), Nil, List(Lit.Int(6)))))))))
//                              ), Term.Name("foreach")
//                            ), List(Term.Name("println"))
//                          ))
//                      )
//                    )
//                  )
//                )
//              )))))

    }
  }
}