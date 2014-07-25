package au.com.dius.pact.provider.gradle

class ProviderInfo {
    String protocol = 'http'
    String host = 'localhost'
    Integer port = 8080
    String path = '/'
    String name

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
}
