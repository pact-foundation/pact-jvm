package au.com.dius.pact.provider.junitsupport.loader;

/**
 * Interface which defines a consumer version selector method with the correct signature
 */
public interface IConsumerVersionSelectors {
    /**
     * Return the consumer version selectors to use in the test
     */
    @au.com.dius.pact.provider.junitsupport.loader.ConsumerVersionSelectors
    SelectorBuilder consumerVersionSelectors(SelectorBuilder builder);
}
