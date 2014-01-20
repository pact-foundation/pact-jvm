package au.com.dius.pact.model.waitingforscalaz

import org.specs2.mutable.Specification

class AlignSpec extends Specification {
  "Align" should {
    val short = List(1)
    val long = List("2","3")


    "align matching lists" in {
      Align(short, short) must beEqualTo(List(Both(1, 1)))
    }

    "align a shorter list to a longer list" in {
      Align(short, long) must beEqualTo(List(Both(1, "2"), That("3")))
    }

    "align a longer list to a shorter list" in {
      Align(long, short) must beEqualTo(List(Both("2", 1), This("3")))
    }
  }
}
