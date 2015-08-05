package au.com.dius.pact.provider.gradle

/**
 * Consumer Info
 */
class ConsumerInfo {
    String name
    def pactFile
    def stateChange
    boolean stateChangeUsesBody = true
    String pactType = 'requestResponse'

    def url(String path) {
        new URL(path)
    }
}
