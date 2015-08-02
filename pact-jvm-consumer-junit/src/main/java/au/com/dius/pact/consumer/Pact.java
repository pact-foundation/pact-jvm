package au.com.dius.pact.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * describes the interactions between a provider and a consumer.
 * The annotated method has to be of following signature:
 *
 * public PactFragment providerDef1(PactDslWithState builder) {...}
 *
 * @author pmucha
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Pact {
    
    /**
     * name of the provider
     */
    String provider();

    /**
     * name of the consumer
     */
    String consumer();

    /**
     * name of the state, the provider has to be in
     * @deprecated Provider state should be defined on the interactions
     */
    @Deprecated
    String state() default "";
}
