package au.com.dius.pact.provider

/**
 * Consumer Info
 */
class ConsumerInfo {
    String name
    def pactFile
    def stateChange
    boolean stateChangeUsesBody = true
    PactVerification verificationType
    List packagesToScan

    def url(String path) {
        new URL(path)
    }
}
