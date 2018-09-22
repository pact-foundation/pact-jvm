package au.com.dius.pact.core.matchers.util

import scala.collection.JavaConversions

object CollectionUtils {

  def toOptionalList(list: java.util.List[String]): Option[List[String]] = {
    if (list == null) {
      None
    } else {
      Some(JavaConversions.collectionAsScalaIterable(list).toList)
    }
  }
}
