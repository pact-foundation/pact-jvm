package au.com.dius.pact.consumer.groovy

import au.com.dius.pact.consumer.PactVerificationResult
import au.com.dius.pact.core.support.SimpleHttp
import groovy.json.JsonSlurper
import spock.lang.Specification

@SuppressWarnings(['AbcMetric'])
class ValuesMatcherPactSpec extends Specification {

  @SuppressWarnings(['NestedBlockDepth'])
  def 'pact test using values matcher'() {
    given:
    def articleService = new PactBuilder()
    articleService {
      serviceConsumer 'ArticleConsumer'
      hasPactWith 'ArticleService'
      port 1244
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
    PactVerificationResult result = articleService.runTest { server, context ->
      def client = new SimpleHttp(server.url)
      def response = client.get()

      assert response.statusCode == 200
      def data = new JsonSlurper().parse(response.inputStream)
      assert data.articles.size() == 1
      assert data.articles[0].variants.size() == 1
      assert data.articles[0].variants[0].keySet() == ['001'] as Set
      assert data.articles[0].variants[0].'001'.size() == 1
      assert data.articles[0].variants[0].'001'[0].bundles.size() == 1
      assert data.articles[0].variants[0].'001'[0].bundles[0].keySet() == ['001-A'] as Set
    }

    then:
    result instanceof PactVerificationResult.Ok
    articleService.interactions.size() == 1
    articleService.interactions[0].response.matchingRules.rulesForCategory('body').matchingRules.keySet() == [
      '$.articles',
      '$.articles[*].variants',
      '$.articles[*].variants[*]',
      '$.articles[*].variants[*].*',
      '$.articles[*].variants[*].*[*].bundles',
      '$.articles[*].variants[*].*[*].bundles[*]',
      '$.articles[*].variants[*].*[*].bundles[*].*.description',
      '$.articles[*].variants[*].*[*].bundles[*].*.referencedArticles',
      '$.articles[*].variants[*].*[*].bundles[*].*.referencedArticles[*].bundleId',
      '$.articles[*].variants[*].*[*].bundles[*].*.referencedArticles[*]',
      '$.articles[*].variants[*].*[*].bundles[*].*.referencedArticles[*].*'
    ] as Set

  }

  @SuppressWarnings(['NestedBlockDepth'])
  def 'key like test'() {
    given:
    def articleService = new PactBuilder()
    articleService {
      serviceConsumer 'ArticleConsumer'
      hasPactWith 'ArticleService'
      port 1244
    }

    articleService {
      uponReceiving('a request for events with useMatchValuesMatcher turned on')
      withAttributes(method: 'get', path: '/')
      willRespondWith(status: 200)
      withBody(mimeType: 'application/json') {
        events {
          keyLike('001') {
            description string('some description')
            eventId identifier()
            references {
              keyLike 'a', eachLike {
                eventId identifier()
              }
            }
          }
        }
      }
    }

    when:
    PactVerificationResult result = articleService.runTest { server, context ->
      def client = new SimpleHttp(server.url)
      def response = client.get()

      assert response.statusCode == 200
      def data = new JsonSlurper().parse(response.inputStream)
      assert data.events.size() == 1
      assert data.events.keySet() == ['001'] as Set
    }

    then:
    result instanceof PactVerificationResult.Ok
    articleService.interactions.size() == 1
    articleService.interactions[0].response.matchingRules.rulesForCategory('body').matchingRules.keySet() == [
      '$.events',
      '$.events.*.description',
      '$.events.*.eventId',
      '$.events.*.references',
      '$.events.*.references.*',
      '$.events.*.references.*[*].eventId'
    ] as Set
  }
}
