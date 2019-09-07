package au.com.dius.pact.matchers.util

import scala.collection.JavaConverters._

object CollectionUtils {

  def toOptionalList(list: java.util.List[String]): Option[List[String]] = {
    if (list == null) {
      None
    } else {
      Some(list.asScala.toList)
    }
  }
}
