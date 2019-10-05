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
   * Authentication scheme to use. Currently bearer and basic are supported. The default is basic.
   */
  String scheme() default "Basic";

  /**
   * Username to use for basic authentication
   */
  String username();

  /**
   * Password to use for basic authentication
   */
  String password();

  /**
   * Token to use for bearer token authentication
   */
  String token() default "";
}
