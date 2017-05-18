package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.broker.PactBrokerClient
import groovy.io.FileType
import org.apache.commons.lang3.StringUtils
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

    @Parameter(defaultValue = 'false')
    private boolean trimSnapshot

    @Parameter(defaultValue = '${project.build.directory}/pacts')
    private String pactDirectory

    @Parameter(required = true)
    private String pactBrokerUrl

    @Parameter
    private String pactBrokerUsername

    @Parameter
    private String pactBrokerPassword

    @Parameter(defaultValue = 'basic')
    private String pactBrokerAuthenticationScheme

    private PactBrokerClient brokerClient

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        if (trimSnapshot) {
            projectVersion -= '-SNAPSHOT'
        }
        if (brokerClient == null) {
          def options = [:]
          if (StringUtils.isNotEmpty(pactBrokerUsername)) {
            options.authentication = [pactBrokerAuthenticationScheme ?: 'basic', pactBrokerUsername, pactBrokerPassword]
          }
          brokerClient = new PactBrokerClient(pactBrokerUrl, options)
        }
        try {
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
        } catch (FileNotFoundException e) {
            println "Pact directory ${pactDirectory} does not exist, skipping uploading of pacts"
        }
    }

}
