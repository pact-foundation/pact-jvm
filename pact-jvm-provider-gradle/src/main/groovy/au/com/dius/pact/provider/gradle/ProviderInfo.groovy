package au.com.dius.pact.provider.gradle

import groovy.json.JsonSlurper
import org.gradle.api.GradleException

class ProviderInfo {
    String protocol = 'http'
    String host = 'localhost'
    Integer port = 8080
    String path = '/'
    String name
    def startProviderTask
    def terminateProviderTask
    def requestFilter

    List<ConsumerInfo> consumers = []

    public ProviderInfo(String name) {
        this.name = name
    }

    public ConsumerInfo hasPactWith(String consumer, Closure closure) {
        def consumerInfo = new ConsumerInfo(name: consumer)
        consumers << consumerInfo
        closure.delegate = consumerInfo
        closure.call(consumerInfo)
        consumerInfo
    }

    public ConsumerInfo[] hasPactsWith(String consumersGroupName, Closure closure) {
        def consumersGroup = new ConsumersGroup(name: consumersGroupName)
        closure.delegate = consumersGroup
        closure(consumersGroup)

        createConsumerListFromPactFiles(consumersGroup)
    }

    private void createConsumerListFromPactFiles(ConsumersGroup consumersGroup) {
        if (!consumersGroup.pactFileLocation) {
            return
        }

        File pactFileDirectory = consumersGroup.pactFileLocation
        if (!pactFileDirectory.exists() || !pactFileDirectory.canRead()) {
            throw new GradleException("pactFileDirectory ${pactFileDirectory.absolutePath} does not exist or is not readable")
        }

        pactFileDirectory.eachFile { File file ->
            consumers << new ConsumerInfo(
                    name: new JsonSlurper().parse(file).consumer.name,
                    pactFile: file,
                    stateChange: consumersGroup.stateChange,
                    stateChangeUsesBody: consumersGroup.stateChangeUsesBody)
        }
    }
}
