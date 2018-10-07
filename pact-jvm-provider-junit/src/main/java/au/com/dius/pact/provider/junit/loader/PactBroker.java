package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.support.expressions.SystemPropertyResolver;
import au.com.dius.pact.support.expressions.ValueResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point {@link au.com.dius.pact.provider.junit.PactRunner} to source of pacts for contract tests
 * Default values can be set by setting the `pactbroker.*` system properties
 *
 * @see PactBrokerLoader pact loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PactSource(PactBrokerLoader.class)
@Inherited
public @interface PactBroker {
    /**
     * @return host of pact broker
     */
    String host() default "${pactbroker.host:}";

    /**
     * @return port of pact broker
     */
    String port() default "${pactbroker.port:}";

    /**
     * HTTP protocol, defaults to http
     * @deprecated Use scheme instead (will be removed in 4.0.x)
     */
    @Deprecated
    String protocol() default "${pactbroker.protocol:http}";

    /**
     * HTTP scheme, defaults to HTTP
     */
    String scheme() default "${pactbroker.protocol:http}";

    /**
     * Tags to use to fetch pacts for, defaults to `latest`
     * If you set the tags through the `pactbroker.tags` system property, separate the tags by commas
     */
    String[] tags() default "${pactbroker.tags:latest}";

    /**
     * Consumers to fetch pacts for, defaults to all consumers
     * If you set the consumers through the `pactbroker.consumers` system property, separate the consumers by commas
     */
    String[] consumers() default "${pactbroker.consumers:}";

  /**
   * If the test should fail if no pacts are found for the provider, default is true
   * @deprecated Use a @IgnoreNoPactsToVerify annotation on the test class instead (will be removed in 4.0.x)
   */
  @Deprecated
  boolean failIfNoPactsFound() default true;

  /**
   * Authentication to use with the pact broker, by default no authentication is used
   */
  PactBrokerAuth authentication() default @PactBrokerAuth(scheme = "${pactbroker.auth.scheme:basic}", username = "${pactbroker.auth.username:}", password = "${pactbroker.auth.password:}");

  /**
   * Override the default value resolver for resolving the values in the expressions
   */
  Class<? extends ValueResolver> valueResolver() default SystemPropertyResolver.class;
}
