package specification

import java.io.File

import au.com.dius.pact.model._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.specs2.specification.Example

abstract class ResponseSpecificationSpec extends SpecificationSpec {

  def test(input: PactResponseSpecification) = {
    val result = ResponseMatching.matchRules(input.expected, input.actual)
    if(input.`match`) {
      result mustEqual FullResponseMatch
    } else {
      result mustNotEqual FullResponseMatch
    }
  }

  def fragments(path: String): Seq[Example] = {
    val resources = getClass.getResource(path)
    val file = new File(resources.toURI)
    file.listFiles().flatMap { folder =>
      if(folder.isDirectory) {
        val dirName = folder.getName
        folder.listFiles(jsonFilter).map { testFile =>
          val fileName = testFile.getName
          implicit val formats = DefaultFormats
          val testJson = parse(testFile)
          val testData = testJson.transformField {
            case ("body", value) => ("body", JString(pretty(value)))
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

}
