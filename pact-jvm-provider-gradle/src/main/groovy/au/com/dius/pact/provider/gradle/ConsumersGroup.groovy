package au.com.dius.pact.provider.gradle

import groovy.transform.ToString

@ToString
class ConsumersGroup {
    def name
    File pactFileLocation
    def stateChange
    boolean stateChangeUsesBody = false

    def url(String path) {
        stateChange = new URL(path)
    }

}
