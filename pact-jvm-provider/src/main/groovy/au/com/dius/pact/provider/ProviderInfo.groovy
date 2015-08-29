package au.com.dius.pact.provider

import groovy.json.JsonSlurper
import groovy.transform.ToString

/**
 * Provider Info Config
 */
@ToString
class ProviderInfo {
    String protocol = 'http'
    def host = 'localhost'
    Integer port = 8080
    String path = '/'
    String name = 'provider'

    def startProviderTask
    def terminateProviderTask

    def requestFilter
    def stateChangeRequestFilter
    def createClient
    boolean insecure = false
    File trustStore
    String trustStorePassword = 'changeit'

    URL stateChangeUrl
    boolean stateChangeUsesBody = true

    PactVerification verificationType = PactVerification.REQUST_RESPONSE
    List packagesToScan = []
    List<ConsumerInfo> consumers = []

    ProviderInfo() {
    }

    ProviderInfo(String name) {
        this.name = name
    }

    ConsumerInfo hasPactWith(String consumer, Closure closure) {
        def consumerInfo = new ConsumerInfo(name: consumer)
        consumers << consumerInfo
        closure.delegate = consumerInfo
        closure.call(consumerInfo)
        consumerInfo
    }

    List hasPactsWith(String consumersGroupName, Closure closure) {
        def consumersGroup = new ConsumersGroup(name: consumersGroupName)
        closure.delegate = consumersGroup
        closure(consumersGroup)

        setupConsumerListFromPactFiles(consumersGroup)
    }

    @SuppressWarnings('ThrowRuntimeException')
    private List setupConsumerListFromPactFiles(ConsumersGroup consumersGroup) {
        if (!consumersGroup.pactFileLocation) {
            return []
        }

        File pactFileDirectory = consumersGroup.pactFileLocation
        if (!pactFileDirectory.exists() || !pactFileDirectory.canRead()) {
            throw new RuntimeException("pactFileDirectory ${pactFileDirectory.absolutePath} " +
                'does not exist or is not readable')
        }

        pactFileDirectory.eachFile { File file ->
            consumers << new ConsumerInfo(
                    name: new JsonSlurper().parse(file).consumer.name,
                    pactFile: file,
                    stateChange: consumersGroup.stateChange,
                    stateChangeUsesBody: consumersGroup.stateChangeUsesBody)
        }
        consumers
    }
}
