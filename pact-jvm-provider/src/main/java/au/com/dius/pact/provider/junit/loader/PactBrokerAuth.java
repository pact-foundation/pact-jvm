package au.com.dius.pact.provider.junit.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the authentication scheme to use with the pact broker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface PactBrokerAuth {

  /**
   * Authentication scheme to use. The default is basic.
   */
  String scheme() default "Basic";

  /**
   * Username to use for authentication
   */
  String username();

  /**
   * Password to use for authentication
   */
  String password();
}
