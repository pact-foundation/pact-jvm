package au.com.dius.pact.matchers

import scala.collection.mutable

object Matchers {

    var matchers = mutable.HashMap[String, Matcher]()

    def registerStandardMatchers() = {
        matchers += ("regex" -> new RegexMatcher())
    }
}

trait Matcher {

}

class RegexMatcher extends Matcher {

}
