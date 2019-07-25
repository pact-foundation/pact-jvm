package au.com.dius.pact.provider.reporters

import au.com.dius.pact.core.model.Request
import au.com.dius.pact.core.model.RequestResponseInteraction
import au.com.dius.pact.core.model.Response
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.ProviderInfo
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

@SuppressWarnings('UnnecessaryObjectReferences')
class JsonReporterSpec extends Specification {

  private File reportDir

  def setup() {
    reportDir = File.createTempDir()
  }

  def cleanup() {
    reportDir.deleteDir()
  }

  def 'does not overwrite the previous report file'() {
    given:
    def reporter = new JsonReporter(reportDir: reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def provider2 = new ProviderInfo(name: 'provider2')

    when:
    reporter.initialise(provider1)
    reporter.finaliseReport()
    reporter.initialise(provider2)
    reporter.finaliseReport()

    then:
    reportDir.list().sort() as List == ['provider1.json', 'provider2.json']
  }

  def 'merges results with an existing file when the provider matches'() {
    given:
    def reporter = new JsonReporter(reportDir: reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def provider2 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())
    def interaction2 = new RequestResponseInteraction('Interaction 2', [], new Request(), new Response())

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, null)
    reporter.interactionDescription(interaction1)
    reporter.finaliseReport()
    reporter.initialise(provider2)
    reporter.reportVerificationForConsumer(consumer, provider2, null)
    reporter.interactionDescription(interaction2)
    reporter.finaliseReport()

    def reportJson = new JsonSlurper().parse(new File(reportDir, 'provider1.json'))

    then:
    reportDir.list().sort() as List == ['provider1.json']
    reportJson.provider.name == 'provider1'
    reportJson.execution.size == 2
    reportJson.execution*.interactions*.interaction.description == [['Interaction 1'], ['Interaction 2']]
  }

  def 'overwrites an existing file when the provider does not match'() {
    given:
    def reporter = new JsonReporter(reportDir: reportDir)
    def provider1 = new ProviderInfo(name: 'provider1')
    def consumer = new ConsumerInfo(name: 'Consumer')
    def interaction1 = new RequestResponseInteraction('Interaction 1', [], new Request(), new Response())
    new File(reportDir, 'provider1.json').text = JsonOutput.toJson([
      provider: [name: 'provider2'],
      execution: [
        [consumer: [name: 'Consumer'],
         interactions: [
           [
             interaction: [
               description: 'Interaction 2',
               request: [method: 'POST', path: '/'],
               response: [status: 200]
             ],
             verification: [result: 'OK']
           ]
         ]
        ]
      ]
    ])

    when:
    reporter.initialise(provider1)
    reporter.reportVerificationForConsumer(consumer, provider1, null)
    reporter.interactionDescription(interaction1)
    reporter.finaliseReport()

    def reportJson = new JsonSlurper().parse(new File(reportDir, 'provider1.json'))

    then:
    reportDir.list().sort() as List == ['provider1.json']
    reportJson.provider.name == 'provider1'
    reportJson.execution.size == 1
    reportJson.execution.first().interactions.first().interaction.description == 'Interaction 1'
  }

}
