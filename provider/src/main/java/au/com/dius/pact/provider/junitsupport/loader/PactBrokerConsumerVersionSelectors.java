package au.com.dius.pact.provider.junitsupport.loader;

import java.lang.annotation.*;

/**
 * Used to mark a method that will set up any consumer version selectors required for a Pact verification test
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface PactBrokerConsumerVersionSelectors { }
