import org.specs2.mutable.Specification

class MainSpec extends Specification {
  "Main method" should {
    "do a thing" in {
      Main.main(Array("foo", "bar")) must beEqualTo("foo")
    }
  }
}