package au.com.dius.pact.provider.junitsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to control the generation of verification reports
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface VerificationReports {

  /**
   * Names of the reports to generate
   */
  String[] value() default "console";

  /**
   * Directory where reports should be written
   */
  String reportDir() default "target/pact/reports";

}
