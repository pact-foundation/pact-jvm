package au.com.dius.pact.provider

import au.com.dius.pact.core.model.FileSource
import au.com.dius.pact.core.model.PactReader
import au.com.dius.pact.core.pactbroker.PactBrokerClient
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Provider Info Config
 */
@ToString
@EqualsAndHashCode
class ProviderInfo implements IProviderInfo {
    String protocol = 'http'
    def host = 'localhost'
    def port = 8080
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
    boolean stateChangeTeardown = false

    boolean isDependencyForPactVerify = true

    PactVerification verificationType
    List packagesToScan = []
    List<IConsumerInfo> consumers = []

    ProviderInfo() {
    }

    ProviderInfo(String name) {
        this.name = name
    }

    ConsumerInfo hasPactWith(String consumer, Closure closure) {
        def consumerInfo = new ConsumerInfo(consumer)
        consumers << consumerInfo
        closure.delegate = consumerInfo
        closure.call(consumerInfo)
        consumerInfo
    }

    List<ConsumerInfo> hasPactsWith(String consumersGroupName, Closure closure) {
        def consumersGroup = new ConsumersGroup(name: consumersGroupName)
        closure.delegate = consumersGroup
        closure(consumersGroup)

        setupConsumerListFromPactFiles(consumersGroup)
    }

    List<ConsumerInfo> hasPactsFromPactBroker(Map options = [:], String pactBrokerUrl) {
      PactBrokerClient client = new PactBrokerClient(pactBrokerUrl, options)
      def consumersFromBroker = client.fetchConsumers(name).collect { ConsumerInfo.from(it) }
      consumers.addAll(consumersFromBroker)
      consumersFromBroker
    }

    List<ConsumerInfo> hasPactsFromPactBrokerWithTag(Map options = [:], String pactBrokerUrl, String tag) {
        PactBrokerClient client = new PactBrokerClient(pactBrokerUrl, options)
        def consumersFromBroker = client.fetchConsumersWithTag(name, tag).collect { ConsumerInfo.from(it) }
        consumers.addAll(consumersFromBroker)
        consumersFromBroker
    }

    @SuppressWarnings('ThrowRuntimeException')
    private List<ConsumerInfo> setupConsumerListFromPactFiles(ConsumersGroup consumersGroup) {
        if (!consumersGroup.pactFileLocation) {
            return []
        }

        File pactFileDirectory = consumersGroup.pactFileLocation
        if (!pactFileDirectory.exists() || !pactFileDirectory.canRead()) {
            throw new RuntimeException("pactFileDirectory ${pactFileDirectory.absolutePath} " +
                'does not exist or is not readable')
        }

        pactFileDirectory.eachFileRecurse { File file ->
            if (file.file && file.name ==~ consumersGroup.include) {
              String name = PactReader.loadPact(file).consumer.name
              consumers << new ConsumerInfo(name,
                consumersGroup.stateChange,
                consumersGroup.stateChangeUsesBody,
                [],
                null,
                new FileSource(file)
              )
            }
        }
        consumers
    }
}
