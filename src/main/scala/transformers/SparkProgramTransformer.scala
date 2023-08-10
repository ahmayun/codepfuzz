package transformers

import transformers.SparkProgramTransformer.treeFromFile

import scala.meta._


case class SparkProgramTransformer(tree: Tree) extends Transformer {

  def this(filePath: String) = {
    this(treeFromFile(filePath))
  }

  def writeTo(p: String): Unit = {
    new java.io.File(p).getParentFile.mkdirs()
    val path = java.nio.file.Paths.get(p)
    java.nio.file.Files.write(path, tree.toString().getBytes())
  }

  // INCOMPLETE:  handle the following case:  import pkg.{Class1, Class2}
  //              break into import pkg.Class1; import pkg.Class2
  def splitImports(): SparkProgramTransformer = {
    this
  }

  // INCOMPLETE: ideally this should be user supplied, but hardcoded for now
  def _replaceImports(toReplace: Map[String, String]): SparkProgramTransformer = {
    SparkProgramTransformer(tree.transform {
      case q"import org.apache.spark.{SparkConf,SparkContext}" => q"import abstraction.{SparkConf,SparkContext}"
      case q"import org.apache.spark.SparkContext" => q"import abstraction.SparkContext"
      case q"import org.apache.spark.SparkConf" => q"import abstraction.SparkConf"
    })
  }

  def replaceImports(toReplace: Map[String, String]): SparkProgramTransformer = {
    splitImports()._replaceImports(toReplace)
  }

  def changePackageTo(pkg: String): SparkProgramTransformer = {
    SparkProgramTransformer(tree.transform {
      case Pkg(_, stats) =>
        Pkg(pkg.parse[Term].get.asInstanceOf[Term.Ref], stats)
    })
  }

  def modifySparkContext(term: Term): Term = {
    Term.New(Init(Type.Name("SparkContextWithDP"), Name(""), List(List(term))))
  }

  def modifyDatasetLoading(term: Term): Term = {
    val Term.Apply(Term.Select(Term.Apply(Term.Select(prefix, _), args), Term.Name("map")), List(parseFunc)) = term
    Term.Apply(Term.Select(prefix, Term.Name("textFileProv")), args :+ parseFunc)
  }

  def attachDFOMonitor(dfo: Term): Term = {
    val Term.Apply(Term.Select(rddName, Term.Name(dfoName)), args) = dfo
    dfoName match {
      case Constants.KEY_JOIN => s"${Constants.MAP_TRANSFORMS(Constants.KEY_JOIN)}($rddName, ${args.mkString(",")}, 0)".parse[Term].get
      case Constants.KEY_GBK => s"${Constants.MAP_TRANSFORMS(Constants.KEY_GBK)}($rddName, 0)".parse[Term].get
      case Constants.KEY_RBK => s"${Constants.MAP_TRANSFORMS(Constants.KEY_RBK)}($rddName, ${args.mkString(",")}, 0)".parse[Term].get
      //      case Constants.KEY_FILTER => s"${Constants.MAP_TRANSFORMS(Constants.KEY_FILTER)}($rddName, ${args.mkString(",")}, $id)".parse[Term].get
      case _ => dfo
    }
  }

  def insertAtEndOfFunction(mainFunc: Defn, statement: Stat): Defn = {
    val Defn.Def(mods, name, tparams, paramss, decltpe, Term.Block(code)) = mainFunc
    Defn.Def(mods, name, tparams, paramss, decltpe, Term.Block(code :+ statement))
  }

  def modifyFunctionArgs(funcName: String, iParam: Int, nameType: (String, String)): SparkProgramTransformer = {
    val (sNewName, sNewType) = nameType
    val custom = new Transformer {
      override def apply(t: Tree): Tree = t match {
        case Defn.Def(fmods, fname@Term.Name(`funcName`), ftparams, fparamss, fdecltpe, fbody) =>
          val newType = sNewType.parse[Type].get
          val Term.Param(pmods, pname, pdecltpe, pdefault) = fparamss.head(iParam)
          val newParam = Term.Param(
            pmods,
            if(sNewName == null) pname else Term.Name(sNewName),
            if(sNewType == null) pdecltpe else Some(newType),
            pdefault
          )
          val modifiedParams = fparamss.head.updated(iParam, newParam)
          Defn.Def(fmods, fname, ftparams, List(modifiedParams), fdecltpe, fbody)
        case node@_ =>
          super.apply(node)
      }
    }
    SparkProgramTransformer(custom(tree))
  }


  def modifyFunctionReturnType(funcName: String, sNewType: String): SparkProgramTransformer = {
    val custom = new Transformer {
      override def apply(t: Tree): Tree = t match {
        case Defn.Def(fmods, fname@Term.Name(`funcName`), ftparams, fparamss, _, fbody) =>
          val newType = sNewType.parse[Type].get
          Defn.Def(fmods, fname, ftparams, fparamss, Some(newType), fbody)
        case node@_ =>
          super.apply(node)
      }
    }
    SparkProgramTransformer(custom(tree))
  }

  def enableTaintProp(): SparkProgramTransformer = {
    val custom = new Transformer {
      override def apply(t: Tree): Tree = t match {
        case node@Term.New(Init(Type.Name("SparkContext"), _, _)) =>
          modifySparkContext(node)
        case node@Term.Apply(Term.Select(Term.Name("SparkContext"), Term.Name("getOrCreate")), _) =>
          modifySparkContext(node)
        case node@Term.Apply(Term.Select(Term.Apply(Term.Select(_, Term.Name("textFile")), _), Term.Name("map")), _) =>
          modifyDatasetLoading(node)
        case Type.Name(name) if Constants.MAP_PRIM2SYM.contains(name) =>
          Type.Name(Constants.MAP_PRIM2SYM(name))
        case node@_ =>
          super.apply(node)
      }
    }

    SparkProgramTransformer(custom(tree))
      .modifyFunctionArgs("main", 0, (null, "Array[String]"))
      .modifyFunctionReturnType("main", "ProvInfo")
      .addImports(
        List(
          "sparkwrapper.SparkContextWithDP",
          "taintedprimitives._",
          "taintedprimitives.SymImplicits._"
        )
      )
  }

  def attachMonitors(): SparkProgramTransformer = {

    val custom = new Transformer {
      override def apply(t: Tree): Tree = t match {
        case node@Term.Apply(Term.Select(_, name), _) if Constants.MAP_TRANSFORMS.contains(name.value) =>
          val term = attachDFOMonitor(node)
          super.apply(term)
        case node@Defn.Def(_, Term.Name("main"), _, _, _, _) =>
          super.apply(insertAtEndOfFunction(node, Constants.CONSOLIDATOR.parse[Stat].get))
//      case Term.If(predicate, ifBody, elseBody) =>
//        val term = attachPredicateMonitor(predicate, id);
//        super.apply(Term.If(term, ifBody, elseBody))
        case node@_ =>
          super.apply(node)
      }
    }

    SparkProgramTransformer(custom(tree))
      .addImports(
        List("fuzzer.ProvInfo")
      )
  }

  def addImports(imports: List[String]): SparkProgramTransformer = {
    SparkProgramTransformer(
      tree.transform {
        case Import(existing) =>
          Import(existing ++ imports.map(_.parse[Importer].get))
        case node@_ =>
          super.apply(node)
      }
    )
  }
}

object SparkProgramTransformer {
  def treeFromFile(p: String): Tree = {
    val path = java.nio.file.Paths.get(p)
    val bytes = java.nio.file.Files.readAllBytes(path)
    val text = new String(bytes, "UTF-8")
    val input = Input.VirtualFile(path.toString, text)
    input.parse[Source].get
  }
}
