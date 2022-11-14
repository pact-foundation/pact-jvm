package au.com.dius.pact.provider.junitsupport.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static au.com.dius.pact.core.support.Auth.DEFAULT_AUTH_HEADER;

/**
 * Defines the authentication scheme to use with the pact broker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface PactBrokerAuth {
  /**
   * Username to use for basic authentication
   */
  String username() default "";

  /**
   * Password to use for basic authentication
   */
  String password() default "";

  /**
   * Token to use for bearer token authentication
   */
  String token() default "";

  /**
   * Override default `Authorization` header with this value
   */
  String headerName() default DEFAULT_AUTH_HEADER;
}
