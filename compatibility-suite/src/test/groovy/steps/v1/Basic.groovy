package steps.v1

import au.com.dius.pact.core.model.Consumer
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.Provider
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.Response
import io.cucumber.java.PendingException
import io.cucumber.java.en.Given

@SuppressWarnings('UnusedMethodParameter')
class Basic {
  Pact pact = new RequestResponsePact(new Provider('basic-v1'), new Consumer('basic-v1'))

  @Given('the following HTTP interactions have been setup:')
  void setupInteractions(List<Map<String, String>> entries) {
    entries.eachWithIndex { entry, index ->
      def request = new Request(entry['method'] ?: 'GET', entry['path'] ?: '/path', [:], [:])
      def response = new Response((entry['response'] ?: '200').toInteger())

      def contentType = entry['response content']
      if (contentType != null && !contentType.isEmpty()) {
        response.headers['content-type'] = [contentType]
      }

      def body = entry['response body']
      if (body != null && !body.isEmpty()) {

      }

      def interaction = new RequestResponseInteraction("interaction $index", [], request, response)
      pact.interactions.add(interaction)
    }
  }
}
