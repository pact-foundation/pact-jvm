package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class V4PactMergeSpec extends Specification {

  private static JsonValue.Object schemaConfig(String schema) {
    new JsonValue.Object(['avroSchema': new JsonValue.StringValue(schema)])
  }

  private static V4Pact pactWithPlugin(String pluginName, String configKey, String schema) {
    def metadata = [
      plugins: [
        [name: pluginName, version: '1.0.0', configuration: [(configKey): schemaConfig(schema)]]
      ]
    ]
    new V4Pact(new Consumer('consumer'), new Provider('provider'), new ArrayList<Interaction>(), metadata)
  }

  def 'mergeInteractions(other) keeps plugin configuration from both pacts when the keys differ'() {
    given:
    def first = pactWithPlugin('avro', 'hash-1', 'schema-1')
    def second = pactWithPlugin('avro', 'hash-2', 'schema-2')

    when:
    def merged = first.mergeInteractions(second)
    def avroPlugin = merged.metadata['plugins'].find { it['name'] == 'avro' }
    def configuration = avroPlugin['configuration'] as Map

    then:
    configuration.keySet() == ['hash-1', 'hash-2'] as Set
  }

  def 'mergeInteractions(other) is a no-op for metadata when the other pact has no plugins'() {
    given:
    def first = pactWithPlugin('avro', 'hash-1', 'schema-1')
    def second = new V4Pact(new Consumer('consumer'), new Provider('provider'), new ArrayList<Interaction>(), [:])

    when:
    def merged = first.mergeInteractions(second)

    then:
    merged.metadata['plugins'] == first.metadata['plugins']
  }
}
