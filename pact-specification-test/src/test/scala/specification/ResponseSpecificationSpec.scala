package specification

import java.io.File
import java.util

import com.typesafe.scalalogging.StrictLogging
import au.com.dius.pact.model._
import groovy.json.JsonSlurper

abstract class ResponseSpecificationSpec extends SpecificationSpec with StrictLogging {

  sequential

  def test(input: PactResponseSpecification) = {
    val result = ResponseMatching.matchRules(input.expected, input.actual)
    logger.debug(s"${input.comment} -> $result")
    if(input.`match`) {
      result mustEqual FullResponseMatch
    } else {
      result mustNotEqual FullResponseMatch
    }
  }

  def extractResponseSpecification(testJson: util.Map[String, AnyRef]) = {
    val expected = PactReader.extractResponse(testJson.get("expected"))
    expected.setDefaultMimeType("application/json")
    val actual = PactReader.extractResponse(testJson.get("actual"))
    actual.setDefaultMimeType("application/json")
    PactResponseSpecification(testJson.get("match").asInstanceOf[Boolean],
      testJson.get("comment").toString, expected, actual)
  }

  def fragments(path: String) = {
    val resources = getClass.getResource(path)
    val file = new File(resources.toURI)
    file.listFiles().flatMap { folder =>
      if(folder.isDirectory) {
        val dirName = folder.getName
        folder.listFiles(jsonFilter).map { testFile =>
          val fileName = testFile.getName
          val testJson = new JsonSlurper().parse(testFile).asInstanceOf[java.util.Map[String, AnyRef]]
          val testData = extractResponseSpecification(testJson)

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
