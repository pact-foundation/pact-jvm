package au.com.dius.pact.provider.junitsupport.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a custom HTTP header to include in all requests to the pact broker.
 * Both name and value support expression syntax (e.g. {@code ${MY_TOKEN:}}).
 *
 * @see PactBroker#customHeaders()
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Inherited
public @interface PactBrokerHttpHeader {
  /**
   * Header name
   */
  String name();

  /**
   * Header value. Supports expression syntax (e.g. {@code ${MY_TOKEN:}}).
   */
  String value();
}
