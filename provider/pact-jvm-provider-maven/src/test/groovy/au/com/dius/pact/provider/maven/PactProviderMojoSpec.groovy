package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelector
import au.com.dius.pact.provider.ConsumerInfo
import au.com.dius.pact.provider.IProviderVerifier
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.settings.Server
import org.apache.maven.settings.Settings
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.apache.maven.settings.crypto.SettingsDecryptionResult
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@SuppressWarnings(['UnnecessaryGetter', 'ClosureAsLastMethodParameter'])
class PactProviderMojoSpec extends Specification {

  private PactProviderMojo mojo

  def setup() {
    mojo = new PactProviderMojo()
    mojo.reports = ['console']
  }

  def 'load pacts from pact broker uses the provider pactBrokerUrl'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, new URL('http://broker:1234'),
      new PactBroker(null, null, null, null)))
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors([:], 'http://broker:1234', []) >>
      [ new Consumer(name: 'test consumer') ]
    list.size() == 1
    list[0].name == 'test consumer'
  }

  def 'load pacts from pact broker uses the configured pactBroker Url'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), null, null, null)))
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors([:], 'http://broker:1234', []) >> [ new Consumer() ]
    list
  }

  def 'load pacts from pact broker uses the configured pactBroker Url over pactBrokerUrl'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, new URL('http://broker:1000'),
      new PactBroker(new URL('http://broker:1234'), null, null, null)))
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors([:], 'http://broker:1234', []) >> [ new Consumer() ]
    list
  }

  def 'load pacts from pact broker uses the configured pactBroker basic authentication'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), null, new PactBrokerAuth('basic', null, 'test', 'test'), null)))
    def list = []
    def map = [authentication: ['basic', 'test', 'test']]

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors(map, 'http://broker:1234', []) >> [
      new Consumer()
    ]
    list
  }

  def 'load pacts from pact broker uses the configured pactBroker bearer authentication'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), null, new PactBrokerAuth('bearer', 'test', null, null), null)))
    def list = []
    def map = [authentication: ['bearer', 'test']]

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors(map, 'http://broker:1234', []) >> [
            new Consumer()
    ]
    list
  }

  def 'load pacts from pact broker uses bearer authentication if token attribute is set without scheme being set'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), null,
        new PactBrokerAuth(null, 'test', null, null), null)))
    def list = []
    def map = [authentication: ['bearer', 'test']]

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors(map, 'http://broker:1234', []) >> [
            new Consumer()
    ]
    list
  }

  def 'load pacts from pact broker for each configured pactBroker tag'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), ['1', '2', '3'], null, null)))
    def list = []
    def selectors = ['1', '2', '3'].collect { new ConsumerVersionSelector(it, true) }

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors([:], 'http://broker:1234', selectors) >> [new Consumer()]
    list.size() == 1
  }

  def 'load pacts from pact broker using the Maven server info if the serverId is set'() {
    given:
    def settings = Mock(Settings)
    mojo.settings = settings
    def decrypter = Mock(SettingsDecrypter)
    mojo.decrypter = decrypter
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), null, null, 'test-server')))
    def list = []
    def serverDetails = new Server(username: 'MavenTest')
    def decryptResult = [getServer: { new Server(password: 'MavenPassword') } ] as SettingsDecryptionResult

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * settings.getServer('test-server') >> serverDetails
    1 * decrypter.decrypt({ it.servers == [serverDetails] }) >> decryptResult
    1 * provider.hasPactsFromPactBrokerWithSelectors([authentication: ['basic', 'MavenTest', 'MavenPassword']],
      'http://broker:1234', []) >> [
      new Consumer()
    ]
    list
  }

  def 'Falls back to the passed in broker config if not set on the provider'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL, null))
    def list = []
    mojo.pactBrokerUrl = 'http://broker:1235'

    when:
    mojo.loadPactsFromPactBroker(provider, list, [authentication: ['bearer', '1234']])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors([authentication: ['bearer', '1234']],
      'http://broker:1235', []) >> [ new Consumer() ]
    list
  }

  def 'configures pending pacts if the option is set'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), ['1', '2', '3'], null, null,
        new EnablePending(['master']))))
    def list = []
    def selectors = ['1', '2', '3'].collect { new ConsumerVersionSelector(it, true) }
    def map = [enablePending: true, providerTags: ['master']]

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    1 * provider.hasPactsFromPactBrokerWithSelectors(map, 'http://broker:1234', selectors) >> [new Consumer()]
    list.size() == 1
  }

  def 'throws an exception if pending pacts enabled and there are no provider tags'() {
    given:
    def provider = Spy(new Provider('TestProvider', null as File, null as URL,
      new PactBroker(new URL('http://broker:1234'), ['1', '2', '3'], null, null,
        new EnablePending([]))))
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list, [:])

    then:
    thrown(MojoFailureException)
  }

  def 'load pacts from multiple directories'() {
    given:
    def dir1 = 'dir1' as File
    def dir2 = 'dir2' as File
    def dir3 = 'dir3' as File
    def provider = new Provider('TestProvider', dir3, null, null)
    provider.pactFileDirectories =  [dir1, dir2]
    def verifier = Mock(IProviderVerifier) {
      verifyProviderReturnResult(provider) >> []
    }
    mojo = Spy(PactProviderMojo) {
      loadPactFiles(provider, _) >> []
      providerVerifier() >> verifier
    }
    mojo.serviceProviders = [ provider ]
    mojo.reports = [ 'console' ]
    mojo.buildDir = new File('/tmp')

    when:
    mojo.execute()

    then:
    1 * mojo.loadPactFiles(provider, dir1) >> []
    1 * mojo.loadPactFiles(provider, dir2) >> []
    1 * mojo.loadPactFiles(provider, dir3) >> [ new ConsumerInfo('mock consumer', dir3) ]
  }

  def 'fail the build if there are no pacts and failIfNoPactsFound is true'() {
    given:
    def provider = new Provider('TestProvider', 'dir' as File, null, null)
    def verifier = Mock(IProviderVerifier) {
      verifyProvider(provider) >> [:]
    }
    mojo = Spy(PactProviderMojo) {
      loadPactFiles(provider, _) >> []
      providerVerifier() >> verifier
    }
    mojo.serviceProviders = [ provider ]
    mojo.failIfNoPactsFound = true
    mojo.reports = [ 'console' ]
    mojo.buildDir = new File('/tmp')

    when:
    mojo.execute()

    then:
    thrown(MojoFailureException)
  }

  def 'do not fail the build if there are no pacts and failIfNoPactsFound is false'() {
    given:
    def provider = new Provider('TestProvider', 'dir' as File, null, null)
    def verifier = Mock(IProviderVerifier) {
      verifyProviderReturnResult(provider) >> []
    }
    mojo = Spy(PactProviderMojo) {
      loadPactFiles(provider, _) >> []
      providerVerifier() >> verifier
    }
    mojo.serviceProviders = [ provider ]
    mojo.failIfNoPactsFound = false
    mojo.reports = [ 'console' ]
    mojo.buildDir = new File('/tmp')

    when:
    mojo.execute()

    then:
    noExceptionThrown()
  }

  @RestoreSystemProperties
  def 'system property pact.verifier.publishResults true when set with systemPropertyVariables' () {
    given:
    def provider = new Provider('TestProvider', 'dir' as File, null, null)
    def verifier = Mock(IProviderVerifier) {
      verifyProviderReturnResult(provider) >> []
    }
    mojo = Spy(PactProviderMojo) {
      loadPactFiles(provider, _) >> []
      providerVerifier() >> verifier
    }
    mojo.serviceProviders = [ provider ]
    mojo.failIfNoPactsFound = false
    mojo.systemPropertyVariables.put('pact.verifier.publishResults', 'true')
    mojo.reports = [ 'console' ]
    mojo.buildDir = new File('/tmp')

    when:
    mojo.execute()

    then:
    noExceptionThrown()
    System.getProperty('pact.verifier.publishResults') == 'true'
  }

  @RestoreSystemProperties
  def 'system property pact.provider.version.trimSnapshot true when set with systemPropertyVariables' () {
    given:
    def provider = new Provider('TestProvider', 'dir1' as File, null, null)
    def verifier = Mock(IProviderVerifier) {
      verifyProviderReturnResult(provider) >> []
    }
    mojo = Spy(PactProviderMojo) {
      loadPactFiles(provider, _) >> []
      providerVerifier() >> verifier
    }
    mojo.serviceProviders = [ provider ]
    mojo.failIfNoPactsFound = false
    mojo.systemPropertyVariables.put('pact.provider.version.trimSnapshot', 'true')
    mojo.reports = [ 'console' ]
    mojo.buildDir = new File('/tmp')

    when:
    mojo.execute()

    then:
    noExceptionThrown()
    System.getProperty('pact.provider.version.trimSnapshot') == 'true'
  }
}
