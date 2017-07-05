package au.com.dius.pact.provider.gradle

import au.com.dius.pact.provider.broker.PactBrokerClient
import groovy.io.FileType
import org.apache.commons.lang3.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction

/**
 * Task to push pact files to a pact broker
 */
@SuppressWarnings('Println')
class PactPublishTask extends DefaultTask {

    @TaskAction
    void publishPacts() {
        if (!project.pact.publish) {
            throw new GradleScriptException('You must add a pact publish configuration to your build before you can ' +
                'use the pactPublish task', null)
        }

        PactPublish pactPublish = project.pact.publish
        if (pactPublish.pactDirectory == null) {
            pactPublish.pactDirectory = project.file("${project.buildDir}/pacts")
        }
        if (pactPublish.version == null) {
            pactPublish.version = project.version
        }

        def options = [:]
        if (StringUtils.isNotEmpty(pactPublish.pactBrokerUsername)) {
          options.authentication = [pactPublish.pactBrokerAuthenticationScheme ?: 'basic',
                                    pactPublish.pactBrokerUsername, pactPublish.pactBrokerPassword]
        }
        def brokerClient = new PactBrokerClient(pactPublish.pactBrokerUrl, options)
        File pactDirectory = pactPublish.pactDirectory as File
        boolean anyFailed = false
        pactDirectory.eachFileMatch(FileType.FILES, ~/.*\.json/) { pactFile ->
          def result
          if (pactPublish.tags) {
            print "Publishing ${pactFile.name} with tags ${pactPublish.tags.join(', ')} ... "
          } else {
            print "Publishing ${pactFile.name} ... "
          }
          result = brokerClient.uploadPactFile(pactFile, pactPublish.version, pactPublish.tags)
          println result
          if (!anyFailed && result.startsWith('FAILED!')) {
            anyFailed = true
          }
        }

        if (anyFailed) {
          throw new GradleScriptException('One or more of the pact files were rejected by the pact broker', null)
        }
    }

}
