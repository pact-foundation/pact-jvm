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
    @Parameter(required = true, readonly = true, defaultValue = '${project.version}')
    private String projectVersion

    @Parameter(defaultValue = '${project.build.directory}/pacts')
    private String pactDirectory

    @Parameter
    private String pactBrokerUrl

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        def brokerClient = new PactBrokerClient(pactBrokerUrl)
        File pactDirectory = pactDirectory as File
        pactDirectory.eachFileMatch(FileType.FILES, ~/.*\.json/) { pactFile ->
            print "Publishing ${pactFile.name} ... "
            println brokerClient.uploadPactFile(pactFile, projectVersion)
        }
    }
}
