package au.com.dius.pact.provider.gradle

class ConsumerInfo {
    String name
    def pactFile
    def stateChange
    boolean stateChangeUsesBody = true

    def url(String path) {
        new URL(path)
    }
}
