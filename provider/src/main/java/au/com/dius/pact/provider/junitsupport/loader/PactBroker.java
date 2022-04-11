package au.com.dius.pact.provider.junitsupport.loader;

import au.com.dius.pact.core.support.expressions.SystemPropertyResolver;
import au.com.dius.pact.core.support.expressions.ValueResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point Pact runner to source of pacts for contract tests
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
   * @return URL of pact broker
   */
  String url() default "${pactbroker.url:}";

  /**
   * @return host of pact broker
   * @deprecated Use url instead
   */
  @Deprecated
  String host() default "${pactbroker.host:}";

  /**
   * @return port of pact broker
   * @deprecated Use url instead
   */
  @Deprecated
  String port() default "${pactbroker.port:}";

  /**
   * HTTP scheme, defaults to HTTP
   * @deprecated Use url instead
   */
  @Deprecated
  String scheme() default "${pactbroker.scheme:http}";

  /**
   * Tags to use to fetch pacts for, defaults to `latest`
   * If you set the tags through the `pactbroker.tags` system property, separate the tags by commas
   *
   * @deprecated Use {@link #consumerVersionSelectors} instead
   */
  @Deprecated
  String[] tags() default "${pactbroker.tags:}";

  /**
   * Consumer version selectors to fetch pacts for, defaults to latest version
   * If you set the version selector tags or latest fields through system properties, separate values by commas
   */
  VersionSelector[] consumerVersionSelectors() default @VersionSelector(
    tag = "${pactbroker.consumerversionselectors.tags:}",
    latest = "${pactbroker.consumerversionselectors.latest:}",
    consumer = "${pactbroker.consumers:}"
  );

  /**
   * Consumers to fetch pacts for, defaults to all consumers
   * If you set the consumers through the `pactbroker.consumers` system property, separate the consumers by commas
   *
   * @deprecated Use {@link #consumerVersionSelectors} instead
   */
  @Deprecated
  String[] consumers() default "${pactbroker.consumers:}";

  /**
   * Authentication to use with the pact broker, by default no authentication is used
   */
  PactBrokerAuth authentication() default @PactBrokerAuth(username = "${pactbroker.auth.username:}",
          password = "${pactbroker.auth.password:}", token = "${pactbroker.auth.token:}");

  /**
   * Override the default value resolver for resolving the values in the expressions
   */
  Class<? extends ValueResolver> valueResolver() default SystemPropertyResolver.class;

  /**
   * If the pending pacts feature should be enabled. This can be set with the pactbroker.enablePending JVM system property.
   * When this is set to true, the provider tags property also needs to be set
   */
  String enablePendingPacts() default "${pactbroker.enablePending:false}";

  /**
   * Provider Tags to use to evaluate pending pacts
   */
  String[] providerTags() default "${pactbroker.providerTags:}";

  /**
   * The earliest date WIP pacts should be included (ex: YYYY-MM-DD). If no date is provided, WIP pacts will not be
   * included.
   */
  String includeWipPactsSince() default "${pactbroker.includeWipPactsSince:}";

  /**
   * Enabling insecure TLS by setting this to true will disable hostname validation and trust all certificates. Use with caution.
   * This can be set with the pactbroker.enableInsecureTls JVM system property.
   */
  String enableInsecureTls() default "${pactbroker.enableInsecureTls:false}";
}
