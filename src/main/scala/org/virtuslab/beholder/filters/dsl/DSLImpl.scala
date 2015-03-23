package org.virtuslab.beholder.filters.dsl

import scala.reflect.macros.whitebox.Context

/**
 * Author: Krzysztof Romanowski
 */
object DSLImpl {
  def create_imp(c: Context)(query: c.Tree)(creation: c.Tree): c.Tree = {
    val extractor = new Extractor(c)
    extractor.implement(
      creation.asInstanceOf[extractor.c.Tree],
      query.asInstanceOf[extractor.c.Tree]
    ).asInstanceOf[c.Tree]
  }

  private class Extractor(val c: Context) {

    import c.universe._

    private object Field {
      def unapply(tree: Tree): Option[(Tree, Tree, Tree, Tree)] = tree match {
        case Apply(Select(Apply(Apply(TypeApply(Select(Apply(rest, List(name)), _), _), List(field)), _), _), List(column)) =>
          Some((rest, name, field, column))
        case _ => None
      }
    }

    var names: List[Tree] = Nil
    var fields: List[Tree] = Nil
    var columns: List[Tree] = Nil

    def implement(code: c.Tree, query: c.Tree): Tree = {
      code match {
        case Function(List(functionArg), Match(selector, List(CaseDef(pattern, guards, body)))) =>
          transform(body, query) {
            newBody =>
              c.untypecheck(Function(List(functionArg), Match(selector, List(CaseDef(pattern, guards, newBody)))))
          }
        case Function(vals, body) =>
          transform(body, query) {
            newBody => Function(vals, newBody)
          }
        case _ =>
          c.abort(c.enclosingPosition, "unsupported tree shape")
      }
    }

    def transform(body: Tree, query: Tree)(funcCreation: Tree => Tree): Tree = {
      newField(body)

      val mappedQuery = {
        val tupleType = c.parse("scala.Tuple" + columns.size)
        val queryMappingFunction = funcCreation(q"$tupleType(..$columns)")
        q"$query.map($queryMappingFunction)"
      }
      val createFun = q"org.virtuslab.beholder.filters.dsl.FilterFactory.crate"

      val tree = Apply(createFun, List(mappedQuery, seq(fields), seq(names)))
      tree
    }

    private def newField(t: Tree): Unit = t match {
      case Field(rest, name, field, column) =>
        names = name :: names
        fields = field :: fields
        columns = column :: columns
        rest match {
          case q"org.virtuslab.beholder.filters.dsl.DSL.EmptyName" =>
          case Select(nextLevel, _) =>
            newField(nextLevel)
          case t =>
            c.error(t.pos, "Unknown tree")
        }
      case tree =>
        c.error(t.pos, "Unknown tree")
    }

    def seq(args: List[Tree]) =
      Apply(Ident(TermName("Seq")), args)
  }
}
