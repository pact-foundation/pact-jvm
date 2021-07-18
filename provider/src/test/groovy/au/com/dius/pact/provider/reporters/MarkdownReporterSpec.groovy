package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.matchers.BodyMismatch
import au.com.dius.pact.core.matchers.HeaderMismatch
import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.provider.BodyComparisonResult
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import com.github.michaelbull.result.Ok
import spock.lang.Issue
import spock.lang.Specification
import au.com.dius.pact.core.support.json.JsonParser

@SuppressWarnings(['UnnecessaryObjectReferences', 'LineLength'])
class MarkdownReporterSpec extends Specification {

  private File reportDir

  def setup() {
    reportDir = File.createTempDir()
  }

  def cleanup() {
    reportDir.deleteDir()
  }

  def 'does not overwrite the previous report file'() {
    given:
    def reporter = new MarkdownReporter('test', reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def provider2 = new ProviderInfo(name: 'provider2')

    when:
    reporter.initialise(provider1)
    reporter.finaliseReport()
    reporter.initialise(provider2)
    reporter.finaliseReport()

    then:
    reportDir.list().sort() as List == ['provider1.md', 'provider2.md']
  }

  def 'appends to an existing report file'() {
    given:
    def reporter = new MarkdownReporter('test', reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('Interaction 2', [], new Request(), new Response())

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, 'staging')
    reporter.interactionDescription(interaction1)
    reporter.finaliseReport()
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, 'production')
    reporter.interactionDescription(interaction2)
    reporter.finaliseReport()

    def results = new File(reportDir, 'provider1.md').text

    then:
    results.contains('## Verifying a pact between _Consumer_ and _provider1_ for tag staging\n\nInteraction 1 ')
    results.contains('## Verifying a pact between _Consumer_ and _provider1_ for tag production\n\nInteraction 2 ')
  }

  def 'does not specify tag if not tag is not specified'() {
    given:
    def reporter = new MarkdownReporter('test', reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, null)
    reporter.interactionDescription(interaction1)
    reporter.finaliseReport()

    def results = new File(reportDir, 'provider1.md').text

    then:
    results.contains('## Verifying a pact between _Consumer_ and _provider1_\n\nInteraction 1 ')
  }

  @SuppressWarnings(['MethodSize', 'TrailingWhitespace'])
  def 'generates the correct markdown for validation failures'() {
    given:
    def reporter = new MarkdownReporter('test', reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, null)
    reporter.interactionDescription(interaction1)
    reporter.statusComparisonFailed(200, 'expected status of 201 but was 200')
    reporter.headerComparisonFailed('HEADER-X', ['Y'], [
      new HeaderMismatch('HEADER-X', 'Y', '', "Expected a header 'HEADER-X' but was missing")
    ])
    reporter.bodyComparisonFailed(
      new Ok(new BodyComparisonResult([
        '$.0': [
          new BodyMismatch(
            JsonParser.parseString('{"doesNotExist":"Test","documentId":0}'),
            JsonParser.parseString('{"documentId":0,"documentCategoryId":5,"documentCategoryCode":null,"contentLength":0,"tags":null}'),
            'Expected doesNotExist="Test" but was missing', '$.0', '''{
              -  "doesNotExist": "Test",
              -  "documentId": 0
              +  "documentId": 0,
              +  "documentCategoryId": 5,
              +  "documentCategoryCode": null,
              +  "contentLength": 0,
              +  "tags": null
              }'''),
          new BodyMismatch(
            JsonParser.parseString('{"doesNotExist":"Test","documentId":0}'),
            JsonParser.parseString('{"documentId":0,"documentCategoryId":5,"documentCategoryCode":null,"contentLength":0,"tags":null}'),
            'Expected doesNotExist="Test" but was missing', '$.0', '''{
                -  "doesNotExist": "Test",
                -  "documentId": 0
                +  "documentId": 0,
                +  "documentCategoryId": 5,
                +  "documentCategoryCode": null,
                +  "contentLength": 0,
                +  "tags": null
                }''')],
        '$.1': [
          new BodyMismatch(JsonParser.parseString('{"doesNotExist":"Test","documentId":0}'),
            JsonParser.parseString('{"documentId":1,"documentCategoryId":5,"documentCategoryCode":null,"contentLength":0,"tags":null}'),
            'Expected doesNotExist="Test" but was missing', '$.1', '''{
              -  "doesNotExist": "Test",
              -  "documentId": 0
              +  "documentId": 1,
              +  "documentCategoryId": 5,
              +  "documentCategoryCode": null,
              +  "contentLength": 0,
              +  "tags": null
              }''')]
      ], [
        '  {',
        '-    " doesNotExist ": " Test ",',
        '-    " documentId ": 0',
        '+    " documentId ": 0,',
        '+    " documentCategoryId ": 5,',
        '+    " documentCategoryCode ": null,',
        '+    " contentLength ": 0,',
        '+    " tags ": null',
        '+  },',
        '+  {',
        '+    " documentId ": 1,',
        '+    " documentCategoryId ": 5,',
        '+    " documentCategoryCode ": null,',
        '+    " contentLength ": 0,',
        '+    " tags ": null',
        '  }'
      ]))
    )
    reporter.finaliseReport()

    def results = new File(reportDir, 'provider1.md').text

    then:
    results.contains(
   '''|&nbsp;&nbsp;&nbsp;&nbsp;has status code **200** (<span style='color:red'>FAILED</span>)
      |
      |```
      |expected status of 201 but was 200
      |```'''.stripMargin()
    )
    results.contains(
   '''|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;"**HEADER-X**" with value "**[Y]**" (<span style='color:red'>FAILED</span>)  
      |
      |```
      |Expected a header 'HEADER-X' but was missing
      |```'''.stripMargin()
    )
    results.contains(
      '''|&nbsp;&nbsp;&nbsp;&nbsp;has a matching body (<span style='color:red'>FAILED</span>)  
         |
         || Path | Failure |
         || ---- | ------- |
         ||`$.0`|Expected doesNotExist="Test" but was missing|
         |||Expected doesNotExist="Test" but was missing|
         ||`$.1`|Expected doesNotExist="Test" but was missing|
         |
         |
         |Diff:
         |
         |```diff
         |  {
         |-    " doesNotExist ": " Test ",
         |-    " documentId ": 0
         |+    " documentId ": 0,
         |+    " documentCategoryId ": 5,
         |+    " documentCategoryCode ": null,
         |+    " contentLength ": 0,
         |+    " tags ": null
         |+  },
         |+  {
         |+    " documentId ": 1,
         |+    " documentCategoryId ": 5,
         |+    " documentCategoryCode ": null,
         |+    " contentLength ": 0,
         |+    " tags ": null
         |  }
         |```'''.stripMargin()
    )
  }

  @Issue('#1128')
  def 'updates the summary with the status of each consumer'() {
    given:
    def reporter = new MarkdownReporter('test', reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def consumer2 = new ConsumerInfo(name: 'Consumer2')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('Interaction 2', [], new Request(), new Response())

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, 'master')
    reporter.interactionDescription(interaction1)
    reporter.finaliseReport()
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer2, provider1, 'master')
    reporter.interactionDescription(interaction2)
    reporter.finaliseReport()

    def results = new File(reportDir, 'provider1.md').text

    then:
    results.contains(
      '''|| Consumer  | Result |
         ||-----------|--------|
         || Consumer  | OK     |
         || Consumer2 | OK     |'''.stripMargin()
    )
  }

  @Issue('#1128')
  def 'updates the summary with interaction failure'() {
    given:
    def reporter = new MarkdownReporter('test', reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def consumer2 = new ConsumerInfo(name: 'Consumer2')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('Interaction 2', [], new Request(), new Response())

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, 'master')
    reporter.interactionDescription(interaction1)
    reporter.finaliseReport()
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer2, provider1, 'master')
    reporter.interactionDescription(interaction2)
    reporter.statusComparisonFailed(200, 'expected status of 201 but was 200')
    reporter.finaliseReport()
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer2, provider1, 'master')
    reporter.interactionDescription(interaction2)
    reporter.finaliseReport()

    def results = new File(reportDir, 'provider1.md').text

    then:
    results.contains(
      '''|| Consumer  | Result |
         ||-----------|--------|
         || Consumer  | OK     |
         || Consumer2 | Failed |'''.stripMargin()
    )
  }

  @Issue('#1128')
  def 'updates the summary with multiple failures'() {
    given:
    def reporter = new MarkdownReporter('test', reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def consumer2 = new ConsumerInfo(name: 'Consumer2')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('Interaction 2', [], new Request(), new Response())

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, 'master')
    reporter.interactionDescription(interaction1)
    reporter.requestFailed(provider1, interaction2, 'Failure 1', new Exception(), false)
    reporter.finaliseReport()
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, 'master')
    reporter.interactionDescription(interaction1)
    reporter.requestFailed(provider1, interaction2, 'Failure 2', new Exception(), false)
    reporter.finaliseReport()
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer2, provider1, 'master')
    reporter.interactionDescription(interaction2)
    reporter.statusComparisonFailed(200, 'expected status of 201 but was 200')
    reporter.finaliseReport()
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer2, provider1, 'master')
    reporter.interactionDescription(interaction2)
    reporter.requestFailed(provider1, interaction2, 'Failure', new Exception(), false)
    reporter.finaliseReport()

    def results = new File(reportDir, 'provider1.md').text

    then:
    results.contains(
      '''|| Consumer  |     Result     |
         ||-----------|----------------|
         || Consumer  | Request failed |
         || Consumer2 | Failed         |'''.stripMargin()
    )
  }
}
