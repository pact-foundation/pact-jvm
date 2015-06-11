package specification

import au.com.dius.pact.model._
import java.io.{FilenameFilter, File}
import org.specs2.SpecificationLike
import org.specs2.matcher.{StandardMatchResults, MustMatchers}
import org.specs2.execute.StandardResults
import org.specs2.specification.{Example, Fragments, FragmentsBuilder}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ResponseSpecificationSpec extends SpecificationLike
  with MustMatchers
  with StandardResults
  with StandardMatchResults
  with FragmentsBuilder {

  val jsonFilter = new FilenameFilter {
    def accept(dir: File, name: String): Boolean = name.endsWith(".json")
  }

  def fragments: Seq[Example] = {
    val resources = getClass.getResource("/response/")
    val file = new File(resources.toURI())
    file.listFiles().flatMap { folder =>
      if(folder.isDirectory) {
        val dirName = folder.getName
        folder.listFiles(jsonFilter).map { testFile =>
          val fileName = testFile.getName
          implicit val formats = DefaultFormats
          val testJson = parse(testFile)
          var testData = testJson.transformField {
            case ("body", value) => ("body", JString(pretty(value)))
            case ("responseMatchingRules", value) => ("matchingRules", value)
            case ("requestMatchingRules", value) => ("matchingRules", value)  
          }.extract[PactResponseSpecification]

          val description = s"$dirName/$fileName ${testData.comment}"
          Example(description, {
            test(testData)
          })
        }
      } else {
        Seq()
      }
    }
  }

  override def is: Fragments = Fragments.create(fragments :_*)

  def test(input: PactResponseSpecification) = {
    val result = ResponseMatching.matchRules(input.expected, input.actual)
    if(input.`match`) {
      result mustEqual FullResponseMatch
    } else {
      result mustNotEqual FullResponseMatch
    }
  }
}
