package au.com.dius.pact.matchers

import scala.collection.Seq

interface MismatchFactory<Mismatch> {
    Mismatch create(def expected, def actual, String message, Seq<String> path)
}
