package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.DslPart
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.consumer.dsl.LambdaDslObject
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

import java.util.function.Consumer

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonArray

@ExtendWith(PactConsumerTestExt)
@PactTestFor(providerName = 'ProviderWith200Items', pactVersion = PactSpecVersion.V3)
@SuppressWarnings(['JUnitPublicNonTestMethod', 'PropertyName', 'UnnecessaryObjectReferences',
  'ClosureAsLastMethodParameter'])
class ArrayWith200ItemsTest {
  String FILE_PATH = 'path'
  String FILE_ID = 'id'
  String FILE_NAME = 'name'
  String FILE_TYPE = 'type'
  String FILE_SIZE = 'size'
  String FILE_RECORD_SIZE = 'record_size'
  String TYPE = 'type'
  String FILES = 'files'

  @Pact(consumer = 'Consumer')
  RequestResponsePact filesPact(PactDslWithProvider builder) {
    builder
      .uponReceiving('a request for 200 items')
        .path('/values')
      .willRespondWith()
        .status(200)
        .body(generateBody())
      .toPact()
  }

  DslPart generateBody() {
    Consumer<LambdaDslObject> generic = { o ->
      o.stringType(FILE_PATH, 'PATH')
      o.stringType(FILE_ID, 'DDDDDD')
      o.stringType(FILE_NAME, 'TITI')
      o.stringType(FILE_TYPE, 'EXAMPLE')
      o.numberType(FILE_SIZE, 2)
      o.numberType(FILE_RECORD_SIZE, 3)
    }
    Consumer<LambdaDslObject> file1 = { o ->
      o.stringValue(FILE_PATH, 'PATH1')
      o.stringValue(FILE_ID, 'AAAA')
      o.stringValue(FILE_NAME, 'TOTO')
      o.stringValue(TYPE, 'TYPE_C')
      o.numberValue(FILE_SIZE, 4)
      o.numberValue(FILE_RECORD_SIZE, 2)
    }
    newJsonArray({ folders ->
      folders.object({ folderA ->
        folderA.array(FILES, { appLambdaDslJsonArray ->
          appLambdaDslJsonArray.object(generic) // item 1
          198.times {
            appLambdaDslJsonArray.object(file1)
          }
          appLambdaDslJsonArray.object(generic) // item 200
        })
      })
    }).build()
  }

  @Test
  void testFiles(MockServer mockServer) {
    HttpResponse httpResponse = Request.Get("${mockServer.url}/values")
      .execute().returnResponse()
    assert httpResponse.statusLine.statusCode == 200
  }
}
