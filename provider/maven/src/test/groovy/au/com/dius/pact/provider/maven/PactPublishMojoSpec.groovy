package au.com.dius.pact.provider.maven

import au.com.dius.pact.core.pactbroker.PactBrokerClient
import au.com.dius.pact.core.pactbroker.PublishConfiguration
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import org.apache.maven.plugin.MojoExecutionException
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files

@SuppressWarnings('LineLength')
class PactPublishMojoSpec extends Specification {

  private PactPublishMojo mojo
  private PactBrokerClient brokerClient

  def setup() {
    brokerClient = Mock(PactBrokerClient)
    mojo = new PactPublishMojo(pactDirectory: 'some/dir', brokerClient: brokerClient, projectVersion: '0.0.0')
  }

  def 'uploads all pacts to the pact broker'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    3.times {
      def file = Files.createTempFile(dir, 'pactfile', '.json')
      file.write(pact)
    }
    mojo.pactDirectory = dir.toString()

    when:
    mojo.execute()

    then:
    3 * brokerClient.uploadPactFile(_, _) >> new Ok(null)

    cleanup:
    dir.deleteDir()
  }

  def 'Fails with an exception if any pacts fail to upload'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    3.times {
      def file = Files.createTempFile(dir, 'pactfile', '.json')
      file.write(pact)
    }
    mojo.pactDirectory = dir.toString()

    when:
    mojo.execute()

    then:
    3 * brokerClient.uploadPactFile(_, _) >> new Ok(null) >>
            new Err(new RuntimeException('FAILED! Bang')) >> new Ok(null)
    thrown(MojoExecutionException)

    cleanup:
    dir.deleteDir()
  }

  def 'if the broker username is set, passes in the creds to the broker client'() {
    given:
    mojo.pactBrokerUsername = 'username'
    mojo.pactBrokerPassword = 'password'
    mojo.brokerClient = null
    mojo.pactBrokerUrl = '/broker'

    when:
    mojo.execute()

    then:
    new PactBrokerClient('/broker', _) >> { args ->
      assert args[1] == [authentication: ['basic', 'username', 'password']]
      brokerClient
    }
  }

  def 'if the broker token is set, it passes in the creds to the broker client'() {
    given:
    mojo.pactBrokerToken = 'token1234'
    mojo.brokerClient = null
    mojo.pactBrokerUrl = '/broker'

    when:
    mojo.execute()

    then:
    new PactBrokerClient('/broker', _) >> { args ->
      assert args[1] == [authentication: ['bearer', 'token1234']]
      brokerClient
    }
  }

    def 'trimSnapshot=true removes the "-SNAPSHOT"'() {
        given:
        mojo.projectVersion = '1.0.0-SNAPSHOT'
        mojo.trimSnapshot = true

        when:
        mojo.execute()

        then:
        assert mojo.projectVersion == '1.0.0'
    }

    def 'trimSnapshot=true removes the last occurrence of "-SNAPSHOT"'() {
        given:
        mojo.projectVersion = projectVersion
        mojo.trimSnapshot = true

        when:
        mojo.execute()

        then:
        assert mojo.projectVersion == result

        where:
        projectVersion                              | result
        '1.0.0-NOT-A-SNAPSHOT-abc-SNAPSHOT'         | '1.0.0-NOT-A-SNAPSHOT-abc'
        '1.0.0-NOT-A-SNAPSHOT-abc-SNAPSHOT-re234hj' | '1.0.0-NOT-A-SNAPSHOT-abc-re234hj'
        '1.0.0-SNAPSHOT-re234hj'                    | '1.0.0-re234hj'
    }

    def 'trimSnapshot=false leaves version unchanged'() {
        given:
        mojo.projectVersion = '1.0.0-SNAPSHOT'
        mojo.trimSnapshot = false

        when:
        mojo.execute()

        then:
        assert mojo.projectVersion == '1.0.0-SNAPSHOT'
    }

    def 'trimSnapshot=true leaves non-snapshot versions unchanged'() {
        given:
        mojo.projectVersion = '1.0.0'
        mojo.trimSnapshot = true

        when:
        mojo.execute()

        then:
        assert mojo.projectVersion == '1.0.0'
    }

  def 'Published the pacts to the pact broker with tags if any tags are specified'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    def file = Files.createTempFile(dir, 'pactfile', '.json')
    file.write(pact)
    mojo.pactDirectory = dir.toString()

    def tags = ['one', 'two', 'three']
    mojo.tags = tags

    when:
    mojo.execute()

    then:
    1 * brokerClient.uploadPactFile(_, new PublishConfiguration('0.0.0', tags)) >> new Ok(null)

    cleanup:
    dir.deleteDir()
  }

  @RestoreSystemProperties
  def 'Tags can also be overridden with Java system properties'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    def file = Files.createTempFile(dir, 'pactfile', '.json')
    file.write(pact)
    mojo.pactDirectory = dir.toString()
    System.setProperty('pact.consumer.tags', '1,2,3')
    mojo.tags = ['one', 'two', 'three']

    when:
    mojo.execute()

    then:
    1 * brokerClient.uploadPactFile(_, new PublishConfiguration('0.0.0', ['1', '2', '3'])) >> new Ok(null)

    cleanup:
    dir.deleteDir()
  }

  def 'Allows some files to be excluded from being published'() {
    given:
    def dir = Files.createTempDirectory('pacts').toFile()
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    def file1 = new File(dir, 'pact.json')
    file1.write(pact)
    def file2 = new File(dir, 'pact-2.json')
    file2.write(pact)
    def file3 = new File(dir, 'pact-3.json')
    file3.write(pact)
    def file4 = new File(dir, 'other-pact.json')
    file4.write(pact)
    mojo.pactDirectory = dir.toString()
    mojo.excludes = [ 'other\\-pact', 'pact\\-\\d+' ]

    when:
    mojo.execute()

    then:
    1 * brokerClient.uploadPactFile(file1, _) >> new Ok(null)
    0 * brokerClient.uploadPactFile(file2, _)
    0 * brokerClient.uploadPactFile(file3, _)
    0 * brokerClient.uploadPactFile(file4, _)

    cleanup:
    dir.deleteDir()
  }

  def 'Allows the branch name to be set'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    def file = Files.createTempFile(dir, 'pactfile', '.json')
    file.write(pact)
    mojo.pactDirectory = dir.toString()

    mojo.branchName = 'feat/test'
    mojo.buildUrl = 'http:/1234'

    when:
    mojo.execute()

    then:
    1 * brokerClient.uploadPactFile(_, new PublishConfiguration('0.0.0', [], 'feat/test', 'http:/1234')) >> new Ok(null)

    cleanup:
    dir.deleteDir()
  }
}
