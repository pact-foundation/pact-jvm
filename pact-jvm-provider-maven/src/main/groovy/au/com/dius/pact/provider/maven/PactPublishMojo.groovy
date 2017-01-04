package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.broker.PactBrokerClient
import groovy.io.FileType
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter

/**
 * Task to push pact files to a pact broker
 */
@Mojo(name = 'publish')
@SuppressWarnings('Println')
class PactPublishMojo extends AbstractMojo {
    @Parameter(required = true, defaultValue = '${project.version}')
    private String projectVersion

    @Parameter(defaultValue = '${project.build.directory}/pacts')
    private String pactDirectory

    @Parameter(required = true)
    private String pactBrokerUrl

    private PactBrokerClient brokerClient

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        if (brokerClient == null) {
          brokerClient = new PactBrokerClient(pactBrokerUrl)
        }
        File pactDirectory = new File(pactDirectory)
        boolean anyFailed = false
        pactDirectory.eachFileMatch(FileType.FILES, ~/.*\.json/) { pactFile ->
            print "Publishing ${pactFile.name} ... "
            def result = brokerClient.uploadPactFile(pactFile, projectVersion)
            println result
            if (!anyFailed && result.startsWith('FAILED!')) {
                anyFailed = true
            }
        }

        if (anyFailed) {
          throw new MojoExecutionException('One or more of the pact files were rejected by the pact broker')
        }
    }
}
