package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.model.PactSpecVersion
import groovyx.net.http.RESTClient
import spock.lang.Specification

import static groovyx.net.http.ContentType.JSON

@SuppressWarnings(['AbcMetric'])
class WildcardPactSpec extends Specification {

  @SuppressWarnings(['NestedBlockDepth'])
  def 'pact test requiring wildcards'() {
    given:
    def articleService = new PactBuilder()
    articleService {
      serviceConsumer 'ArticleConsumer'
      hasPactWith 'ArticleService'
      port 1234
    }

    articleService {
      uponReceiving('a request for an article')
      withAttributes(method: 'get', path: '/')
      willRespondWith(status: 200)
      withBody(mimeType: JSON.toString()) {
        articles eachLike {
          variants eachLike {
            keyLike '001', eachLike {
              bundles eachLike {
                keyLike('001-A') {
                  description string('some description')
                  referencedArticles eachLike {
                    bundleId identifier()
                    keyLike '001-A-1', identifier()
                  }
                }
              }
            }
          }
        }
      }
    }

    when:
    PactVerificationResult result = articleService.runTest(specificationVersion: PactSpecVersion.V3) {
      def client = new RESTClient(it.url)
      def response = client.get(requestContentType: JSON)

      assert response.status == 200
      assert response.data.articles.size() == 1
      assert response.data.articles[0].variants.size() == 1
      assert response.data.articles[0].variants[0].keySet() == ['001'] as Set
      assert response.data.articles[0].variants[0].'001'.size() == 1
      assert response.data.articles[0].variants[0].'001'[0].bundles.size() == 1
      assert response.data.articles[0].variants[0].'001'[0].bundles[0].keySet() == ['001-A'] as Set
    }

    then:
    result == PactVerificationResult.Ok.INSTANCE
    articleService.interactions.size() == 1
    articleService.interactions[0].response.matchingRules == [
      '$.body.articles': [match: 'type'],
      '$.body.articles[*].variants': [match: 'type'],
      '$.body.articles[*].variants[*].*': [match: 'type'],
      '$.body.articles[*].variants[*].*[*].bundles': [match: 'type'],
      '$.body.articles[*].variants[*].*[*].bundles[*].*': [match: 'type'],
      '$.body.articles[*].variants[*].*[*].bundles[*].*.description': [match: 'type'],
      '$.body.articles[*].variants[*].*[*].bundles[*].*.referencedArticles': [match: 'type'],
      '$.body.articles[*].variants[*].*[*].bundles[*].*.referencedArticles[*].bundleId': [match: 'type'],
      '$.body.articles[*].variants[*].*[*].bundles[*].*.referencedArticles[*].*': [match: 'type']
    ]

  }
}
