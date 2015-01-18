package au.com.dius.pact.matchers

trait Matcher {
    def domatch[Mismatch](matcherDef: Map[String, String], path: String, expected: Any, actual: Any,
                mismatchFactory: MismatchFactory[Mismatch]) : List[Mismatch]

    def valueOf(value: Any) = {
        value match {
            case null => "null"
            case s: String => "'" + value + "'"
            case _ => value.toString
        }
    }
}
