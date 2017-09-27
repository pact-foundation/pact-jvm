package au.com.dius.pact.provider.maven

import org.apache.maven.settings.Server
import org.apache.maven.settings.Settings
import org.apache.maven.settings.crypto.SettingsDecrypter
import org.apache.maven.settings.crypto.SettingsDecryptionResult
import spock.lang.Specification

@SuppressWarnings(['UnnecessaryGetter', 'ClosureAsLastMethodParameter'])
class PactProviderMojoSpec extends Specification {

  private PactProviderMojo mojo

  def setup() {
    mojo = new PactProviderMojo()
  }

  def 'load pacts from pact broker uses the provider pactBrokerUrl'() {
    given:
    def provider = Mock(Provider) {
      getPactBrokerUrl() >> new URL('http://broker:1234')
      getPactBroker() >> new PactBroker()
    }
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list)

    then:
    1 * provider.hasPactsFromPactBroker([:], 'http://broker:1234') >> [ new Consumer(name: 'test consumer') ]
    list.size() == 1
    list[0].name == 'test consumer'
  }

  def 'load pacts from pact broker uses the configured pactBroker Url'() {
    given:
    def provider = Mock(Provider) {
      getPactBrokerUrl() >> null
      getPactBroker() >> new PactBroker(new URL('http://broker:1234'))
    }
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list)

    then:
    1 * provider.hasPactsFromPactBroker([:], 'http://broker:1234') >> [ new Consumer() ]
    list
  }

  def 'load pacts from pact broker uses the configured pactBroker Url over pactBrokerUrl'() {
    given:
    def provider = Mock(Provider) {
      getPactBrokerUrl() >> new URL('http://broker:1000')
      getPactBroker() >> new PactBroker(new URL('http://broker:1234'))
    }
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list)

    then:
    1 * provider.hasPactsFromPactBroker([:], 'http://broker:1234') >> [ new Consumer() ]
    list
  }

  def 'load pacts from pact broker uses the configured pactBroker authentication'() {
    given:
    def provider = Mock(Provider) {
      getPactBrokerUrl() >> null
      getPactBroker() >> new PactBroker(new URL('http://broker:1234'), null, new BasicAuth('test', 'test'))
    }
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list)

    then:
    1 * provider.hasPactsFromPactBroker([authentication: ['basic', 'test', 'test']], 'http://broker:1234') >> [
      new Consumer()
    ]
    list
  }

  def 'load pacts from pact broker for each configured pactBroker tag'() {
    given:
    def provider = Mock(Provider) {
      getPactBrokerUrl() >> null
      getPactBroker() >> new PactBroker(new URL('http://broker:1234'), ['1', '2', '3'])
    }
    def list = []

    when:
    mojo.loadPactsFromPactBroker(provider, list)

    then:
    1 * provider.hasPactsFromPactBrokerWithTag([:], 'http://broker:1234', '1') >> [new Consumer()]
    1 * provider.hasPactsFromPactBrokerWithTag([:], 'http://broker:1234', '2') >> []
    1 * provider.hasPactsFromPactBrokerWithTag([:], 'http://broker:1234', '3') >> []
    list.size() == 1
  }

  def 'load pacts from pact broker using the Maven server info if the serverId is set'() {
    given:
    def settings = Mock(Settings)
    mojo.settings = settings
    def decrypter = Mock(SettingsDecrypter)
    mojo.decrypter = decrypter
    def provider = Mock(Provider) {
      getPactBrokerUrl() >> null
      getPactBroker() >> new PactBroker(new URL('http://broker:1234'), null, null, 'test-server')
    }
    def list = []
    def serverDetails = new Server(username: 'MavenTest')
    def decryptResult = [getServer: { new Server(password: 'MavenPassword') } ] as SettingsDecryptionResult

    when:
    mojo.loadPactsFromPactBroker(provider, list)

    then:
    1 * settings.getServer('test-server') >> serverDetails
    1 * decrypter.decrypt({ it.servers == [serverDetails] }) >> decryptResult
    1 * provider.hasPactsFromPactBroker([authentication: ['basic', 'MavenTest', 'MavenPassword']],
      'http://broker:1234') >> [
      new Consumer()
    ]
    list
  }

}
