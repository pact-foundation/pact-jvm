package au.com.dius.pact.provider.junitsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * With this annotation set on the test class, the pact runner will ignore the fact that there are no
 * pacts to verify.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface IgnoreNoPactsToVerify {
  /**
   * Boolean flag to indicate that IO errors should also be ignored
   */
  String ignoreIoErrors() default "${pact.verification.ignoreIoErrors:false}";
}
