package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class V4PactMergeSpec extends Specification {

  private static V4Pact pactWithPluginConfig(String pluginName, String configKey, Map<String, JsonValue> fields) {
    def metadata = [
      plugins: [
        [name: pluginName, version: '1.0.0', configuration: [(configKey): new JsonValue.Object(fields)]]
      ]
    ]
    new V4Pact(new Consumer('consumer'), new Provider('provider'), new ArrayList<Interaction>(), metadata)
  }

  private static V4Pact pactWithPlugin(String pluginName, String configKey, String schema) {
    pactWithPluginConfig(pluginName, configKey, ['avroSchema': new JsonValue.StringValue(schema)])
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

  def 'mergeInteractions(other) deep-merges a colliding configuration key even when one side is a disk-loaded pact'() {
    given: 'a pact as PactReader leaves it: configuration values already unwrapped into plain Kotlin types'
    def diskLoadedMetadata = [
      plugins: [
        [name: 'avro', version: '1.0.0', configuration: ['hash-1': [avroSchema: 'schema-1']]]
      ]
    ]
    def diskLoaded = new V4Pact(new Consumer('consumer'), new Provider('provider'), new ArrayList<Interaction>(),
      diskLoadedMetadata)
    and: 'a pact as PactBuilder produces it in memory: configuration values are still JsonValue, same key'
    def inMemory = pactWithPluginConfig('avro', 'hash-1', ['recordName': new JsonValue.StringValue('Item')])

    when:
    def merged = diskLoaded.mergeInteractions(inMemory)
    def avroPlugin = merged.metadata['plugins'].find { it['name'] == 'avro' }
    def configuration = avroPlugin['configuration'] as Map
    def entry = Json.toJson(configuration['hash-1']).asObject().entries

    then: 'both fields survive a structural merge, rather than the second pact silently replacing the first'
    entry.keySet() == ['avroSchema', 'recordName'] as Set
  }
}
