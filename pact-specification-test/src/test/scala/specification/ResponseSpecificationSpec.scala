package specification

import java.io.File

import au.com.dius.pact.model._
import org.json4s._
import org.json4s.jackson.JsonMethods._

abstract class ResponseSpecificationSpec extends SpecificationSpec {

  def test(input: PactResponseSpecification) = {
    val result = ResponseMatching.matchRules(input.expected, input.actual)
    if(input.`match`) {
      result mustEqual FullResponseMatch
    } else {
      result mustNotEqual FullResponseMatch
    }
  }

  def fragments(path: String) = {
    val resources = getClass.getResource(path)
    val file = new File(resources.toURI)
    file.listFiles().flatMap { folder =>
      if(folder.isDirectory) {
        val dirName = folder.getName
        folder.listFiles(jsonFilter).map { testFile =>
          val fileName = testFile.getName
          implicit val formats = DefaultFormats
          val testJson = parse(testFile)
          val transformedJson: JValue = testJson.transformField {
            case ("body", value) => ("body", JString(pretty(value)))
          }
          val testData = transformedJson.extract[PactResponseSpecification].copy(
            actual = PactSerializer.extractResponse(transformedJson \ "actual"),
            expected = PactSerializer.extractResponse(transformedJson \ "expected"))

          val description = s"$dirName/$fileName ${testData.comment}"
          fragmentFactory.example(description, {
            test(testData)
          })
        }
      } else {
        Seq()
      }
    }
  }

}
