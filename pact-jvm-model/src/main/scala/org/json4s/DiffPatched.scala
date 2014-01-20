package org.json4s

/** Computes a diff between two JSONs.
  */
object DiffPatched {

  /** Return a diff.
    * <p>
    * Example:<pre>
    * val Diff(c, a, d) = ("name", "joe") ~ ("age", 10) diff ("fname", "joe") ~ ("age", 11)
    * c = JObject(("age",JInt(11)) :: Nil)
    * a = JObject(("fname",JString("joe")) :: Nil)
    * d = JObject(("name",JString("joe")) :: Nil)
    * </pre>
    */
  def diff(val1: JValue, val2: JValue): Diff = (val1, val2) match {
    case (x, y) if x == y => Diff(JNothing, JNothing, JNothing)
    case (JObject(xs), JObject(ys)) => diffFields(xs, ys)
    case (JArray(xs), JArray(ys)) => diffVals(xs, ys)
    case (JInt(x), JInt(y)) if (x != y) => Diff(JInt(y), JNothing, JNothing)
    case (JDouble(x), JDouble(y)) if (x != y) => Diff(JDouble(y), JNothing, JNothing)
    case (JDecimal(x), JDecimal(y)) if (x != y) => Diff(JDecimal(y), JNothing, JNothing)
    case (JString(x), JString(y)) if (x != y) => Diff(JString(y), JNothing, JNothing)
    case (JBool(x), JBool(y)) if (x != y) => Diff(JBool(y), JNothing, JNothing)
    case (JNothing, x) => Diff(JNothing, x, JNothing)
    case (x, JNothing) => Diff(JNothing, JNothing, x)
    case (x, y) => Diff(y, JNothing, JNothing)
  }

  private def diffFields(vs1: List[JField], vs2: List[JField]) = {
    def diffRec(xleft: List[JField], yleft: List[JField]): Diff = xleft match {
      case Nil => Diff(JNothing, if (yleft.isEmpty) JNothing else JObject(yleft), JNothing)
      case x :: xs => yleft find (_._1 == x._1) match {
        case Some(y) =>
          val Diff(c1, a1, d1) = diff(x._2, y._2).toField(y._1)
          val Diff(c2, a2, d2) = diffRec(xs, yleft filterNot (_ == y))
          Diff(c1 merge c2, a1 merge a2, d1 merge d2)
        case None =>
          val Diff(c, a, d) = diffRec(xs, yleft)
          Diff(c, a, JObject(x :: Nil) merge d)
      }
    }

    diffRec(vs1, vs2)
  }

  private def diffVals(vs1: List[JValue], vs2: List[JValue]) = {
    def diffRec(xleft: List[JValue], yleft: List[JValue]): Diff = (xleft, yleft) match {
      case (xs, Nil) => Diff(JNothing, JNothing, if (xs.isEmpty) JNothing else JArray(xs))
      case (Nil, ys) => Diff(JNothing, if (ys.isEmpty) JNothing else JArray(ys), JNothing)
      case (x :: xs, y :: ys) =>
        val Diff(c1, a1, d1) = diff(x, y)
        val Diff(c2, a2, d2) = diffRec(xs, ys)
        Diff(c1 ++ c2, a1 ++ a2, d1 ++ d2)
    }

    diffRec(vs1, vs2)
  }

  private[json4s] trait Diffable { this: org.json4s.JsonAST.JValue =>
    /** Return a diff.
      * @see org.json4s.Diff#diff
      */

    override def diff(other: JValue) = Diff.diff(this, other)
  }
}
