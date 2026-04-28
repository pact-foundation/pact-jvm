package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.matchingrules.MatchingRules
import au.com.dius.pact.core.model.matchingrules.MatchingRulesImpl
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.v4.MessageContents
import au.com.dius.pact.core.model.v4.V4InteractionType
import au.com.dius.pact.core.support.Result
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class V4InteractionSpec extends Specification {

  // ---- AsynchronousMessage.asV3Interaction (existing test) ----

  def 'when downgrading message to V4, rename the matching rules from content to body'() {
    given:
    MatchingRules matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('content').addRule('$', TypeMatcher.INSTANCE)
    def message = new V4Interaction.AsynchronousMessage('key', 'description',
      new MessageContents(OptionalBody.missing(), [:], matchingRules))

    when:
    def v3Message = message.asV3Interaction()

    then:
    v3Message.toMap(PactSpecVersion.V3).matchingRules == [body: ['$': [matchers: [[match: 'type']], combine: 'AND']]]
  }

  // ---- V4Interaction base class ----

  def 'isV4 returns true'() {
    expect:
    new V4Interaction.SynchronousHttp('key', 'test').V4
  }

  def 'conflictsWith always returns false'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')
    def other = new V4Interaction.SynchronousHttp('key2', 'other')

    expect:
    !interaction.conflictsWith(other)
  }

  def 'uniqueKey returns the key when set'() {
    expect:
    new V4Interaction.SynchronousHttp('my-key', 'test').uniqueKey() == 'my-key'
  }

  def 'uniqueKey falls back to generateKey when key is null'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp(null, 'test')

    expect:
    interaction.uniqueKey() == interaction.generateKey()
  }

  def 'uniqueKey falls back to generateKey when key is empty string'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('', 'test')

    expect:
    interaction.uniqueKey() == interaction.generateKey()
  }

  def 'addTextComment creates a text array with the first comment'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    when:
    interaction.addTextComment('first comment')

    then:
    interaction.comments['text'] instanceof JsonValue.Array
    interaction.comments['text'].values.size() == 1
    interaction.comments['text'].values[0].toString() == 'first comment'
  }

  def 'addTextComment appends subsequent comments to the existing array'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')
    interaction.addTextComment('first')

    when:
    interaction.addTextComment('second')

    then:
    interaction.comments['text'].values.size() == 2
    interaction.comments['text'].values[1].toString() == 'second'
  }

  def 'setTestName sets the testname entry in comments'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    when:
    interaction.setTestName('MySpec.myTest')

    then:
    interaction.comments['testname'].toString() == 'MySpec.myTest'
  }

  def 'addPluginConfiguration stores configuration for a new plugin'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    when:
    interaction.addPluginConfiguration('test-plugin', [version: new JsonValue.StringValue('1.0')])

    then:
    interaction.pluginConfiguration.containsKey('test-plugin')
    interaction.pluginConfiguration['test-plugin']['version'].toString() == '1.0'
  }

  def 'addPluginConfiguration merges configuration for an existing plugin'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')
    interaction.addPluginConfiguration('test-plugin', [v1: new JsonValue.StringValue('one')])

    when:
    interaction.addPluginConfiguration('test-plugin', [v2: new JsonValue.StringValue('two')])

    then:
    interaction.pluginConfiguration['test-plugin'].containsKey('v1')
    interaction.pluginConfiguration['test-plugin'].containsKey('v2')
  }

  def 'addReference creates a references entry in comments for the first call'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    when:
    interaction.addReference('openapi', 'operationId', 'createUser')

    then:
    interaction.comments.containsKey('references')
    def refs = interaction.comments['references'] as JsonValue.Object
    refs.entries.containsKey('openapi')
    def group = refs.entries['openapi'] as JsonValue.Object
    group.entries['operationId'].toString() == 'createUser'
  }

  def 'addReference adds a second name-value pair to an existing group'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')
    interaction.addReference('openapi', 'operationId', 'createUser')

    when:
    interaction.addReference('openapi', 'tag', 'user')

    then:
    def group = (interaction.comments['references'] as JsonValue.Object).entries['openapi'] as JsonValue.Object
    group.entries['operationId'].toString() == 'createUser'
    group.entries['tag'].toString() == 'user'
  }

  def 'addReference handles multiple independent groups'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    when:
    interaction.addReference('openapi', 'operationId', 'createUser')
    interaction.addReference('jira', 'ticket', 'PROJ-123')

    then:
    def refs = interaction.comments['references'] as JsonValue.Object
    (refs.entries['openapi'] as JsonValue.Object).entries['operationId'].toString() == 'createUser'
    (refs.entries['jira'] as JsonValue.Object).entries['ticket'].toString() == 'PROJ-123'
  }

  def 'addReference stores non-string values using Json conversion'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    when:
    interaction.addReference('metadata', 'version', 3)

    then:
    def refs = interaction.comments['references'] as JsonValue.Object
    (refs.entries['metadata'] as JsonValue.Object).entries['version'].toString() == '3'
  }

  // ---- SynchronousHttp ----

  def 'SynchronousHttp generateKey is deterministic from description and provider states'() {
    given:
    def i1 = new V4Interaction.SynchronousHttp(null, 'test interaction', [new ProviderState('state A')])
    def i2 = new V4Interaction.SynchronousHttp(null, 'test interaction', [new ProviderState('state A')])
    def i3 = new V4Interaction.SynchronousHttp(null, 'different description', [new ProviderState('state A')])

    expect:
    i1.generateKey() == i2.generateKey()
    i1.generateKey() != i3.generateKey()
  }

  def 'SynchronousHttp withGeneratedKey returns a copy with the calculated key set'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp(null, 'test')

    when:
    def result = interaction.withGeneratedKey()

    then:
    result instanceof V4Interaction.SynchronousHttp
    result.key == interaction.generateKey()
  }

  def 'SynchronousHttp toMap includes all required fields'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('my-key', 'test interaction')

    when:
    def map = interaction.toMap(PactSpecVersion.V4)

    then:
    map['type'] == 'Synchronous/HTTP'
    map['key'] == 'my-key'
    map['description'] == 'test interaction'
    map['pending'] == false
    map.containsKey('request')
    map.containsKey('response')
  }

  def 'SynchronousHttp toMap omits optional fields when not set'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    when:
    def map = interaction.toMap(PactSpecVersion.V4)

    then:
    !map.containsKey('providerStates')
    !map.containsKey('comments')
    !map.containsKey('pluginConfiguration')
    !map.containsKey('interactionMarkup')
    !map.containsKey('transport')
  }

  def 'SynchronousHttp toMap includes providerStates when set'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test', [new ProviderState('state A')])

    when:
    def map = interaction.toMap(PactSpecVersion.V4)

    then:
    map['providerStates'] == [[name: 'state A']]
  }

  def 'SynchronousHttp toMap includes comments when set'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')
    interaction.addTextComment('a comment')

    when:
    def map = interaction.toMap(PactSpecVersion.V4)

    then:
    map.containsKey('comments')
  }

  def 'SynchronousHttp toMap includes interactionMarkup when set'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test', [], new HttpRequest(), new HttpResponse(),
      null, [:], false, [:], new InteractionMarkup('# Title', 'commonmark'), null)

    when:
    def map = interaction.toMap(PactSpecVersion.V4)

    then:
    map['interactionMarkup'] == [markup: '# Title', markupType: 'commonmark']
  }

  def 'SynchronousHttp toMap includes transport when set'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test', [], new HttpRequest(), new HttpResponse(),
      null, [:], false, [:], new InteractionMarkup(), 'grpc')

    when:
    def map = interaction.toMap(PactSpecVersion.V4)

    then:
    map['transport'] == 'grpc'
  }

  @Unroll
  def 'SynchronousHttp isInteractionType returns #expected for #type'() {
    expect:
    new V4Interaction.SynchronousHttp('key', 'test').isInteractionType(type) == expected

    where:
    type                                   | expected
    V4InteractionType.SynchronousHTTP      | true
    V4InteractionType.AsynchronousMessages | false
    V4InteractionType.SynchronousMessages  | false
  }

  def 'SynchronousHttp isSynchronousRequestResponse returns true'() {
    expect:
    new V4Interaction.SynchronousHttp('key', 'test').synchronousRequestResponse
  }

  def 'SynchronousHttp asSynchronousRequestResponse returns self'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    expect:
    interaction.asSynchronousRequestResponse().is(interaction)
  }

  def 'SynchronousHttp asV4Interaction returns self'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test')

    expect:
    interaction.asV4Interaction().is(interaction)
  }

  def 'SynchronousHttp asV3Interaction creates a RequestResponseInteraction preserving fields'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test', [new ProviderState('state A')],
      new HttpRequest('GET', '/foo'), new HttpResponse(200))

    when:
    def v3 = interaction.asV3Interaction()

    then:
    v3 instanceof RequestResponseInteraction
    v3.description == 'test'
    v3.providerStates == [new ProviderState('state A')]
    v3.request.method == 'GET'
    v3.request.path == '/foo'
    v3.response.status == 200
  }

  def 'SynchronousHttp toString includes PENDING marker when pending flag is set'() {
    given:
    def interaction = new V4Interaction.SynchronousHttp('key', 'test interaction', [], new HttpRequest(),
      new HttpResponse(), null, [:], true)

    expect:
    interaction.toString().contains('[PENDING]')
  }

  def 'SynchronousHttp toString does not include PENDING marker when not pending'() {
    expect:
    !new V4Interaction.SynchronousHttp('key', 'test').toString().contains('[PENDING]')
  }

  def 'SynchronousHttp validateForVersion returns an empty list for a default interaction'() {
    expect:
    new V4Interaction.SynchronousHttp('key', 'test').validateForVersion(PactSpecVersion.V4).empty
  }

  // ---- AsynchronousMessage ----

  def 'AsynchronousMessage generateKey is deterministic from description and provider states'() {
    given:
    def m1 = new V4Interaction.AsynchronousMessage('key', 'test message', new MessageContents(), null,
      [new ProviderState('state A')])
    def m2 = new V4Interaction.AsynchronousMessage('key', 'test message', new MessageContents(), null,
      [new ProviderState('state A')])
    def m3 = new V4Interaction.AsynchronousMessage('key', 'different description')

    expect:
    m1.generateKey() == m2.generateKey()
    m1.generateKey() != m3.generateKey()
  }

  def 'AsynchronousMessage withGeneratedKey returns a copy with the calculated key set'() {
    given:
    def msg = new V4Interaction.AsynchronousMessage(null, 'test message')

    when:
    def result = msg.withGeneratedKey()

    then:
    result instanceof V4Interaction.AsynchronousMessage
    result.key == msg.generateKey()
  }

  def 'AsynchronousMessage toMap includes all required fields'() {
    given:
    def msg = new V4Interaction.AsynchronousMessage('m-key', 'test message')

    when:
    def map = msg.toMap(PactSpecVersion.V4)

    then:
    map['type'] == 'Asynchronous/Messages'
    map['key'] == 'm-key'
    map['description'] == 'test message'
    map['pending'] == false
  }

  def 'AsynchronousMessage toMap includes transport when set'() {
    given:
    def msg = new V4Interaction.AsynchronousMessage('key', 'test', new MessageContents(), null, [], [:], false,
      [:], new InteractionMarkup(), 'kafka')

    when:
    def map = msg.toMap(PactSpecVersion.V4)

    then:
    map['transport'] == 'kafka'
  }

  @Unroll
  def 'AsynchronousMessage isInteractionType returns #expected for #type'() {
    expect:
    new V4Interaction.AsynchronousMessage('key', 'test').isInteractionType(type) == expected

    where:
    type                                   | expected
    V4InteractionType.AsynchronousMessages | true
    V4InteractionType.SynchronousHTTP      | false
    V4InteractionType.SynchronousMessages  | false
  }

  def 'AsynchronousMessage isAsynchronousMessage returns true'() {
    expect:
    new V4Interaction.AsynchronousMessage('key', 'test').asynchronousMessage
  }

  def 'AsynchronousMessage asAsynchronousMessage returns self'() {
    given:
    def msg = new V4Interaction.AsynchronousMessage('key', 'test')

    expect:
    msg.asAsynchronousMessage().is(msg)
  }

  def 'AsynchronousMessage asV4Interaction returns self'() {
    given:
    def msg = new V4Interaction.AsynchronousMessage('key', 'test')

    expect:
    msg.asV4Interaction().is(msg)
  }

  def 'AsynchronousMessage withMetadata sets metadata on contents and returns self'() {
    given:
    def msg = new V4Interaction.AsynchronousMessage('key', 'test')

    when:
    def result = msg.withMetadata([contentType: 'application/json'])

    then:
    result.is(msg)
    msg.contents.metadata == [contentType: 'application/json']
  }

  def 'AsynchronousMessage toString includes PENDING marker when pending flag is set'() {
    given:
    def msg = new V4Interaction.AsynchronousMessage('key', 'test message', new MessageContents(), null, [], [:], true)

    expect:
    msg.toString().contains('[PENDING]')
  }

  def 'AsynchronousMessage toString does not include PENDING marker when not pending'() {
    expect:
    !new V4Interaction.AsynchronousMessage('key', 'test').toString().contains('[PENDING]')
  }

  def 'AsynchronousMessage delegates matchingRules to contents'() {
    given:
    MatchingRules rules = new MatchingRulesImpl()
    rules.addCategory('content').addRule('$', TypeMatcher.INSTANCE)
    def contents = new MessageContents(OptionalBody.missing(), [:], rules)
    def msg = new V4Interaction.AsynchronousMessage('key', 'test', contents)

    expect:
    msg.matchingRules.is(contents.matchingRules)
  }

  def 'AsynchronousMessage delegates metadata to contents'() {
    given:
    def metadata = [contentType: 'application/json']
    def contents = new MessageContents(OptionalBody.missing(), metadata)
    def msg = new V4Interaction.AsynchronousMessage('key', 'test', contents)

    expect:
    msg.metadata.is(contents.metadata)
  }

  def 'AsynchronousMessage delegates messageContents to contents body'() {
    given:
    def body = OptionalBody.body('test'.bytes)
    def contents = new MessageContents(body)
    def msg = new V4Interaction.AsynchronousMessage('key', 'test', contents)

    expect:
    msg.messageContents == body
  }

  def 'AsynchronousMessage validateForVersion returns an empty list for default contents'() {
    expect:
    new V4Interaction.AsynchronousMessage('key', 'test').validateForVersion(PactSpecVersion.V4).empty
  }

  // ---- SynchronousMessages ----

  def 'SynchronousMessages generateKey is deterministic from description and provider states'() {
    given:
    def s1 = new V4Interaction.SynchronousMessages(null, 'sync test', null, [new ProviderState('state A')])
    def s2 = new V4Interaction.SynchronousMessages(null, 'sync test', null, [new ProviderState('state A')])
    def s3 = new V4Interaction.SynchronousMessages(null, 'different description')

    expect:
    s1.generateKey() == s2.generateKey()
    s1.generateKey() != s3.generateKey()
  }

  def 'SynchronousMessages withGeneratedKey returns a copy with the calculated key set'() {
    given:
    def sync = new V4Interaction.SynchronousMessages(null, 'sync test')

    when:
    def result = sync.withGeneratedKey()

    then:
    result instanceof V4Interaction.SynchronousMessages
    result.key == sync.generateKey()
  }

  def 'SynchronousMessages toMap throws an IllegalArgumentException for non-V4 pact versions'() {
    given:
    def sync = new V4Interaction.SynchronousMessages('key', 'sync test')

    when:
    sync.toMap(PactSpecVersion.V3)

    then:
    thrown(IllegalArgumentException)
  }

  def 'SynchronousMessages toMap includes all required fields'() {
    given:
    def sync = new V4Interaction.SynchronousMessages('s-key', 'sync test')

    when:
    def map = sync.toMap(PactSpecVersion.V4)

    then:
    map['type'] == 'Synchronous/Messages'
    map['key'] == 's-key'
    map['description'] == 'sync test'
    map['pending'] == false
    map.containsKey('request')
    map.containsKey('response')
  }

  def 'SynchronousMessages toMap includes transport when set'() {
    given:
    def sync = new V4Interaction.SynchronousMessages('key', 'sync test', null, [], [:], false,
      new MessageContents(), [], [:], new InteractionMarkup(), 'kafka')

    when:
    def map = sync.toMap(PactSpecVersion.V4)

    then:
    map['transport'] == 'kafka'
  }

  @Unroll
  def 'SynchronousMessages isInteractionType returns #expected for #type'() {
    expect:
    new V4Interaction.SynchronousMessages('key', 'test').isInteractionType(type) == expected

    where:
    type                                   | expected
    V4InteractionType.SynchronousMessages  | true
    V4InteractionType.SynchronousHTTP      | false
    V4InteractionType.AsynchronousMessages | false
  }

  def 'SynchronousMessages isSynchronousMessages returns true'() {
    expect:
    new V4Interaction.SynchronousMessages('key', 'test').synchronousMessages
  }

  def 'SynchronousMessages asSynchronousMessages returns self'() {
    given:
    def sync = new V4Interaction.SynchronousMessages('key', 'test')

    expect:
    sync.asSynchronousMessages().is(sync)
  }

  def 'SynchronousMessages asV4Interaction returns self'() {
    given:
    def sync = new V4Interaction.SynchronousMessages('key', 'test')

    expect:
    sync.asV4Interaction().is(sync)
  }

  def 'SynchronousMessages validateForVersion returns an empty list for default request and response'() {
    expect:
    new V4Interaction.SynchronousMessages('key', 'test').validateForVersion(PactSpecVersion.V4).empty
  }

  // ---- interactionFromJson ----

  def 'interactionFromJson returns an error when the type attribute is missing'() {
    when:
    def result = V4Interaction.interactionFromJson(0, new JsonValue.Object([:]), UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Err
  }

  def 'interactionFromJson returns an error for an unknown interaction type'() {
    given:
    def json = new JsonValue.Object([type: new JsonValue.StringValue('Unknown/Type')])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Err
  }

  def 'interactionFromJson parses a SynchronousHTTP interaction'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/HTTP'),
      key: new JsonValue.StringValue('test-key'),
      description: new JsonValue.StringValue('test interaction'),
      pending: JsonValue.False.INSTANCE,
      request: new JsonValue.Object([method: new JsonValue.StringValue('GET'), path: new JsonValue.StringValue('/')]),
      response: new JsonValue.Object([status: new JsonValue.Integer(200)])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value instanceof V4Interaction.SynchronousHttp
    result.value.description == 'test interaction'
    result.value.key == 'test-key'
    !result.value.pending
  }

  def 'interactionFromJson parses an AsynchronousMessages interaction'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Asynchronous/Messages'),
      key: new JsonValue.StringValue('msg-key'),
      description: new JsonValue.StringValue('test message'),
      pending: JsonValue.False.INSTANCE
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value instanceof V4Interaction.AsynchronousMessage
    result.value.description == 'test message'
    result.value.key == 'msg-key'
  }

  def 'interactionFromJson parses a SynchronousMessages interaction with request and response'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/Messages'),
      key: new JsonValue.StringValue('sync-key'),
      description: new JsonValue.StringValue('sync interaction'),
      pending: JsonValue.False.INSTANCE,
      request: new JsonValue.Object([:]),
      response: new JsonValue.Array([new JsonValue.Object([:])])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value instanceof V4Interaction.SynchronousMessages
    result.value.description == 'sync interaction'
    result.value.key == 'sync-key'
    result.value.response.size() == 1
  }

  def 'interactionFromJson defaults to empty request and response for SynchronousMessages when absent'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/Messages'),
      key: new JsonValue.StringValue('sync-key'),
      description: new JsonValue.StringValue('sync interaction'),
      pending: JsonValue.False.INSTANCE
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value instanceof V4Interaction.SynchronousMessages
    result.value.response.empty
  }

  def 'interactionFromJson sets the pending flag from JSON'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/HTTP'),
      key: new JsonValue.StringValue('key'),
      description: new JsonValue.StringValue('test'),
      pending: JsonValue.True.INSTANCE,
      request: new JsonValue.Object([method: new JsonValue.StringValue('GET'), path: new JsonValue.StringValue('/')]),
      response: new JsonValue.Object([status: new JsonValue.Integer(200)])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value.pending
  }

  def 'interactionFromJson parses providerStates from JSON'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/HTTP'),
      key: new JsonValue.StringValue('key'),
      description: new JsonValue.StringValue('test'),
      pending: JsonValue.False.INSTANCE,
      providerStates: new JsonValue.Array([
        new JsonValue.Object([name: new JsonValue.StringValue('state A'), params: new JsonValue.Object([:])])
      ]),
      request: new JsonValue.Object([method: new JsonValue.StringValue('GET'), path: new JsonValue.StringValue('/')]),
      response: new JsonValue.Object([status: new JsonValue.Integer(200)])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value.providerStates == [new ProviderState('state A')]
  }

  def 'interactionFromJson parses comments from a JSON object'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/HTTP'),
      key: new JsonValue.StringValue('key'),
      description: new JsonValue.StringValue('test'),
      pending: JsonValue.False.INSTANCE,
      comments: new JsonValue.Object([testname: new JsonValue.StringValue('myTest')]),
      request: new JsonValue.Object([method: new JsonValue.StringValue('GET'), path: new JsonValue.StringValue('/')]),
      response: new JsonValue.Object([status: new JsonValue.Integer(200)])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value.comments['testname'].toString() == 'myTest'
  }

  def 'interactionFromJson ignores comments that are not a JSON object'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/HTTP'),
      key: new JsonValue.StringValue('key'),
      description: new JsonValue.StringValue('test'),
      pending: JsonValue.False.INSTANCE,
      comments: new JsonValue.Array([]),
      request: new JsonValue.Object([method: new JsonValue.StringValue('GET'), path: new JsonValue.StringValue('/')]),
      response: new JsonValue.Object([status: new JsonValue.Integer(200)])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value.comments == [:]
  }

  def 'interactionFromJson parses pluginConfiguration from JSON'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/HTTP'),
      key: new JsonValue.StringValue('key'),
      description: new JsonValue.StringValue('test'),
      pending: JsonValue.False.INSTANCE,
      pluginConfiguration: new JsonValue.Object([
        'my-plugin': new JsonValue.Object([setting: new JsonValue.StringValue('value')])
      ]),
      request: new JsonValue.Object([method: new JsonValue.StringValue('GET'), path: new JsonValue.StringValue('/')]),
      response: new JsonValue.Object([status: new JsonValue.Integer(200)])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value.pluginConfiguration.containsKey('my-plugin')
    result.value.pluginConfiguration['my-plugin']['setting'].toString() == 'value'
  }

  def 'interactionFromJson parses interactionMarkup from JSON'() {
    given:
    def json = new JsonValue.Object([
      type: new JsonValue.StringValue('Synchronous/HTTP'),
      key: new JsonValue.StringValue('key'),
      description: new JsonValue.StringValue('test'),
      pending: JsonValue.False.INSTANCE,
      interactionMarkup: new JsonValue.Object([
        markup: new JsonValue.StringValue('# Title'),
        markupType: new JsonValue.StringValue('commonmark')
      ]),
      request: new JsonValue.Object([method: new JsonValue.StringValue('GET'), path: new JsonValue.StringValue('/')]),
      response: new JsonValue.Object([status: new JsonValue.Integer(200)])
    ])

    when:
    def result = V4Interaction.interactionFromJson(0, json, UnknownPactSource.INSTANCE)

    then:
    result instanceof Result.Ok
    result.value.interactionMarkup.markup == '# Title'
    result.value.interactionMarkup.markupType == 'commonmark'
  }
}
