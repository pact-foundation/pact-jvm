package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.provider.junit.PactRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point {@link PactRunner} to source of pacts for contract tests
 *
 * @see PactBrokerLoader pact loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PactSource(PactBrokerLoader.class)
public @interface PactBroker {
    /**
     * @return host of pact broker
     */
    String host();

    /**
     * @return port of pact broker
     */
    String port();

    /**
     * HTTP protocol, defaults to http
     */
    String protocol() default "http";

    /**
     * Tags to use to fetch pacts for
     */
    String[] tags() default "latest";

  /**
   * If the test should fail if no pacts are found for the provider, default is true.
   */
  boolean failIfNoPactsFound() default true;
}
