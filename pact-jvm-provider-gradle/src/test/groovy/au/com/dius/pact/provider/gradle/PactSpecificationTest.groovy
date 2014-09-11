package au.com.dius.pact.provider.gradle

import au.com.dius.pact.model.Response
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.codehaus.groovy.runtime.powerassert.PowerAssertionError
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import java.util.jar.JarEntry
import java.util.jar.JarInputStream

import static junit.framework.TestCase.fail

@RunWith(Parameterized)
class PactSpecificationTest {

  private final String spec

  @Parameters
  static specs() {
    def pactSpecJar = System.getProperty('java.class.path').split(System.getProperty('path.separator')).find {
      it =~ /pact-specification-test_.*\.jar$/
    }

    URL jar = new URL('file://' + pactSpecJar)
    JarInputStream zip = new JarInputStream(jar.openStream())
    JarEntry e = zip.getNextJarEntry()
    def specs = []
    while (e != null) {
      if (e.name.endsWith('.json') && e.name.startsWith('response')) {
        specs << (['/' + e.name] as String[])
      }
      e = zip.getNextJarEntry()
    }

    specs
  }

  public PactSpecificationTest(String spec) {
    this.spec = spec
  }

  @Test
  void 'test spec'() {
    try {
      def specData = new JsonSlurper().parse(getClass().getResource(spec))
      def response = Response.apply(specData.expected.status ?: 200, specData.expected.headers ?: [:],
        new JsonBuilder(specData.expected.body ?: [:]).toPrettyString(), [:])
      def actualMimeType = 'application/json'
      if (specData.actual?.headers != null && specData.actual?.headers['Content-Type'] != null) {
        actualMimeType = specData.actual.headers['Content-Type']
      }
      def contentTypeHeader = [getValue: { actualMimeType }] as Header
      def entity = [getContentType: { contentTypeHeader }] as HttpEntity
      def mockResponse = [getEntity: { entity }] as HttpResponse
      def result = ResponseComparison.compareResponse(response, mockResponse, specData.status ?: 200,
        specData.actual.headers ?: [:], specData.actual.body ?: [:])
      if (specData.match) {
        assert result.method && result.headers.every { k, v -> v } && result.body.isEmpty()
      } else {
        assert result.method != true || !result.headers.isEmpty() || !result.body.isEmpty()
      }
    } catch (PowerAssertionError e) {
      throw new Exception(spec + ':\n' + e.message, e)
    } catch (e) {
      throw new Exception(spec + ': ' + e.message, e)
    }
  }

}
