package au.com.dius.pact.provider

import au.com.dius.pact.model.Consumer
import groovy.transform.Canonical

/**
 * Consumer Info
 */
@Canonical
class ConsumerInfo {
    String name
    def pactFile
    def stateChange
    boolean stateChangeUsesBody = true
    PactVerification verificationType
    List packagesToScan
    List pactFileAuthentication

    def url(String path) {
        new URL(path)
    }

    Consumer toPactConsumer() {
      new Consumer(name)
    }
}
