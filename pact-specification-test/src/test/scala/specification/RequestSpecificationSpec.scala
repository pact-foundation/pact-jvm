package specification

import java.io.File

import com.typesafe.scalalogging.StrictLogging
import au.com.dius.pact.model._
import groovy.json.JsonSlurper

abstract class RequestSpecificationSpec extends SpecificationSpec with StrictLogging {

  sequential

  def test(input: PactRequestSpecification) = {
    val fakeInteraction = new Interaction("", null, input.expected, new Response(200))
    val result = RequestMatching.compareRequest(fakeInteraction, input.actual)
    logger.debug(s"${input.comment} -> $result")
    if(input.`match`) {
      result mustEqual FullRequestMatch(fakeInteraction)
    } else {
      result mustNotEqual FullRequestMatch(fakeInteraction)
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
          val testJson = new JsonSlurper().parse(testFile).asInstanceOf[java.util.Map[String, AnyRef]]
          val testData = extractRequestSpecification(testJson)

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

  def extractRequestSpecification(testJson: java.util.Map[String, AnyRef]): PactRequestSpecification = {
    PactRequestSpecification(testJson.get("match").asInstanceOf[Boolean],
      testJson.get("comment").toString,
      PactReader.extractRequestV2(testJson.get("expected")),
      PactReader.extractRequestV2(testJson.get("actual")))
  }
}
