package au.com.dius.pact.provider.gradle

import groovy.io.FileType
import groovy.json.JsonSlurper
import groovyx.net.http.HTTPBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleScriptException
import org.gradle.api.tasks.TaskAction

import static groovyx.net.http.ContentType.JSON
import static groovyx.net.http.Method.PUT

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

        def http = new HTTPBuilder(pactPublish.pactBrokerUrl)
        File pactDirectory = pactPublish.pactDirectory as File
        pactDirectory.eachFileMatch(FileType.FILES, ~/.*\.json/) { pactFile ->
            print "Publishing ${pactFile.name} ... "
            def pact = new JsonSlurper().parse(pactFile)
            http.request(PUT) {
                uri.path = "/pacts/provider/${pact.provider.name}/consumer/${pact.consumer.name}/version/" +
                    "${project.version}"
                requestContentType = JSON
                body = pactFile.text

                response.success = { resp ->
                    println resp.statusLine
                }
            }
        }
    }

}
