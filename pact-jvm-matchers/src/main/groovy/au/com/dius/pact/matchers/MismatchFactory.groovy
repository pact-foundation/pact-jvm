package au.com.dius.pact.matchers

import scala.collection.Seq

/**
 * Interface to a factory class to create a mismatch
 * @param <Mismatch> Type of mismatch to create
 */
@SuppressWarnings('FactoryMethodName')
interface MismatchFactory<Mismatch> {
    Mismatch create(def expected, def actual, String message, Seq<String> path)
}
