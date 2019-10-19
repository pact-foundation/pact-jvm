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
   * Authentication scheme to use.
   *
   * @deprecated Does no longer need to be set explicitly. It is now automatically set to
   * <ul>
   *     <li><b>Basic</b> if username is set.</li>
   *     <li><b>Bearer</b> if token is set.</li>
   * </ul>
   */
  @Deprecated
  String scheme() default "";

  /**
   * Username to use for authentication
   */
  String username() default "";

  /**
   * Password to use for authentication
   */
  String password() default "";
}
