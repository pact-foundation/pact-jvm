package specification

import java.io.{File, FilenameFilter}

import org.specs2.SpecificationLike
import org.specs2.execute.StandardResults
import org.specs2.matcher.{StandardMatchResults, MustMatchers}
import org.specs2.specification.FragmentsBuilder

abstract class SpecificationSpec extends SpecificationLike
  with MustMatchers
  with StandardResults
  with StandardMatchResults
  with FragmentsBuilder {

  val jsonFilter = new FilenameFilter {
    def accept(dir: File, name: String): Boolean = name.endsWith(".json")
  }

}
