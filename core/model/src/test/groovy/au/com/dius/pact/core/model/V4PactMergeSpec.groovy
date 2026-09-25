package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification

class V4PactMergeSpec extends Specification {

  private static V4Pact pactWithMetadata(Map metadata) {
    new V4Pact(new Consumer('consumer'), new Provider('provider'), [], metadata)
  }

  private static V4Pact pactWithPluginConfig(String pluginName, String configKey, Map<String, JsonValue> fields,
                                             String version = '1.0.0') {
    pactWithMetadata([
      plugins: [
        [name: pluginName, version: version, configuration: [(configKey): new JsonValue.Object(fields)]]
      ]
    ])
  }

  private static V4Pact pactWithPlugin(String pluginName, String configKey, String schema) {
    pactWithPluginConfig(pluginName, configKey, ['avroSchema': new JsonValue.StringValue(schema)])
  }

  private static Map pluginEntry(Pact pact, String name) {
    pact.metadata['plugins'].find { it instanceof Map && it['name'] == name }
  }

  def 'mergePact keeps plugin configuration from both pacts when the keys differ'() {
    given:
    def first = pactWithPlugin('avro', 'hash-1', 'schema-1')
    def second = pactWithPlugin('avro', 'hash-2', 'schema-2')

    when:
    def merged = first.mergePact(second)
    def configuration = pluginEntry(merged, 'avro')['configuration'] as Map

    then:
    configuration.keySet() == ['hash-1', 'hash-2'] as Set
  }

  def 'mergePact is a no-op for metadata when the other pact has no plugins'() {
    given:
    def first = pactWithPlugin('avro', 'hash-1', 'schema-1')
    def second = pactWithMetadata([:])

    when:
    def merged = first.mergePact(second)

    then:
    merged.metadata['plugins'] == first.metadata['plugins']
  }

  def 'mergePact deep-merges a colliding configuration key even when one side is a disk-loaded pact'() {
    given: 'a pact as PactReader leaves it: configuration values already unwrapped into plain Kotlin types'
    def diskLoaded = pactWithMetadata([
      plugins: [
        [name: 'avro', version: '1.0.0', configuration: ['hash-1': [avroSchema: 'schema-1']]]
      ]
    ])
    and: 'a pact as PactBuilder produces it in memory: configuration values are still JsonValue, same key'
    def inMemory = pactWithPluginConfig('avro', 'hash-1', ['recordName': new JsonValue.StringValue('Item')])

    when:
    def merged = diskLoaded.mergePact(inMemory)
    def configuration = pluginEntry(merged, 'avro')['configuration'] as Map
    def entry = Json.toJson(configuration['hash-1']).asObject().entries

    then: 'both fields survive a structural merge, rather than the second pact silently replacing the first'
    entry.keySet() == ['avroSchema', 'recordName'] as Set
  }

  def 'mergePact does not duplicate array values when the same configuration is merged more than once'() {
    given:
    def includes = new JsonValue.Array([new JsonValue.StringValue('a')])
    def pact = pactWithPluginConfig('protobuf', 'hash-1', [includes: includes])

    when:
    def merged = pact.mergePact(pact).mergePact(pact)
    def configuration = pluginEntry(merged, 'protobuf')['configuration'] as Map

    then:
    configuration['hash-1'] == new JsonValue.Object([includes: includes])
    merged.metadata['plugins'].size() == 1
  }

  def 'mergePact uses the plugin version from the other pact'() {
    given:
    def first = pactWithPluginConfig('protobuf', 'hash-1', [:], '0.3.0')
    def second = pactWithPluginConfig('protobuf', 'hash-2', [:], '0.5.0')

    when:
    def merged = first.mergePact(second)

    then:
    pluginEntry(merged, 'protobuf')['version'] == '0.5.0'
  }

  def 'mergePact keeps plugin entries without a name or that are not maps'() {
    given:
    def first = pactWithMetadata([plugins: ['invalid', [version: '1.0.0'], [version: '2.0.0']]])
    def second = pactWithPlugin('avro', 'hash-1', 'schema-1')

    when:
    def merged = first.mergePact(second)

    then:
    merged.metadata['plugins'] == ['invalid', [version: '1.0.0'], [version: '2.0.0']] +
      second.metadata['plugins']
  }

  def 'mergePact handles the plugins stored as a JSON array'() {
    given:
    def first = pactWithMetadata([
      plugins: Json.toJson([[name: 'avro', version: '1.0.0', configuration: ['hash-1': [avroSchema: 'schema-1']]]])
    ])
    def second = pactWithPlugin('avro', 'hash-2', 'schema-2')

    when:
    def merged = first.mergePact(second)
    def configuration = pluginEntry(merged, 'avro')['configuration'] as Map

    then:
    configuration.keySet() == ['hash-1', 'hash-2'] as Set
  }

  def 'PactMerge merges the plugin configuration when the existing pact has no interactions'() {
    given:
    def existing = pactWithPlugin('avro', 'hash-1', 'schema-1')
    def newPact = new V4Pact(new Consumer('consumer'), new Provider('provider'), [
      new V4Interaction.SynchronousHttp('key', 'test interaction')
    ], pactWithPlugin('avro', 'hash-2', 'schema-2').metadata)

    when:
    def result = PactMerge.merge(newPact, existing)
    def configuration = pluginEntry(result.result, 'avro')['configuration'] as Map

    then:
    result.ok
    result.result.interactions.size() == 1
    configuration.keySet() == ['hash-1', 'hash-2'] as Set
  }
}
