package au.com.dius.pact.matchers

import java.util.function.{Predicate, ToIntFunction}

import au.com.dius.pact.model.matchingrules.{MatchingRule, MatchingRules}
import com.typesafe.scalalogging.StrictLogging
import io.gatling.jsonpath.AST._
import io.gatling.jsonpath.Parser

import scala.collection.JavaConversions._

object Matchers extends StrictLogging {

  def matchesToken(pathElement: String, token: PathToken) = token match {
    case RootNode => if (pathElement == "$") 2 else 0
    case Field(name) => if (pathElement == name) 2 else 0
    case ArrayRandomAccess(indices) => if (pathElement.matches("\\d+") && indices.contains(pathElement.toInt)) 2 else 0
    case ArraySlice(None, None, 1) => if (pathElement.matches("\\d+")) 1 else 0
    case AnyField => 1
    case _ => 0
  }

  def matchesPath(pathExp: String, path: Seq[String]) =
    new Parser().compile(pathExp) match {
      case Parser.Success(q, _) =>
        val filter = path.reverse.tails.filter(l =>
          l.reverse.corresponds(q)((pathElement, pathToken) => matchesToken(pathElement, pathToken) != 0))
        if (filter.nonEmpty) {
          filter.maxBy(seq => seq.length).length
        } else {
          0
        }
      case ns: Parser.NoSuccess =>
        logger.warn(s"Path expression $pathExp is invalid, ignoring: $ns")
        0
    }

  def calculatePathWeight(pathExp: String, path: Seq[String]) = {
    new Parser().compile(pathExp) match {
      case Parser.Success(q, _) =>
        path.zip(q).map(entry => matchesToken(entry._1, entry._2)).product
      case ns: Parser.NoSuccess =>
        logger.warn(s"Path expression $pathExp is invalid, ignoring: $ns")
        0
    }
  }

  def resolveMatchers(matchers: MatchingRules, category: String, items: Seq[String]) =
    if (category == "body")
      matchers.rulesForCategory(category).filter(new Predicate[String] {
        override def test(p: String): Boolean = matchesPath(p, items) > 0
      })
    else if (category == "header" || category == "query")
      matchers.rulesForCategory(category).filter(new Predicate[String] {
        override def test(p: String): Boolean = items == Seq(p)
      })
    else matchers.rulesForCategory(category)

  def matcherDefined(category: String, path: Seq[String], matchers: MatchingRules): Boolean =
    if (matchers != null)
      resolveMatchers(matchers, category, path).isNotEmpty
    else
      false

  def wildcardMatcherDefined(path: Seq[String], category: String, matchers: MatchingRules): Boolean = {
    if (matchers != null) {
      val resolvedMatchers = matchers.rulesForCategory(category).filter(new Predicate[String] {
        override def test(p: String): Boolean = matchesPath(p, path) == path.length
      })
      asScalaSet(resolvedMatchers.getMatchingRules.keySet()).exists(entry => entry.endsWith(".*"))
    } else
      false
  }

  def domatch[Mismatch](matchers: MatchingRules, category: String, path: Seq[String], expected: Any, actual: Any,
                        mismatchFn: MismatchFactory[Mismatch]) : List[Mismatch] = {
    val matcherDef = selectBestMatcher(matchers, category, path)
    asScalaBuffer(MatcherExecutorKt.domatch(matcherDef, path, expected, actual, mismatchFn)).toList
  }

  def selectBestMatcher[Mismatch](matchers: MatchingRules, category: String, path: Seq[String]) = {
    val matcherCategory = resolveMatchers(matchers, category, path)
    if (category == "body")
      matcherCategory.maxBy(new ToIntFunction[String] {
        override def applyAsInt(value: String): Int = calculatePathWeight(value, path)
      })
    else
      matcherCategory.getMatchingRules.iterator.next()._2
  }
}
