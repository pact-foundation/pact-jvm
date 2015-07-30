package au.com.dius.pact.provider.gradle

/**
 * Config for stage change URL
 */
class StateChangeUrl {

    boolean enabled = false
    String protocol = 'http'
    String host = 'localhost'
    Integer port = 8080
    String path = '/enterState'

}
