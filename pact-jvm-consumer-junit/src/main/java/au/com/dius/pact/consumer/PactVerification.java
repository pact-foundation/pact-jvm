package au.com.dius.pact.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Before each test, a mock server will be setup at given port/host that will provide mocked responses.
 * after each test, it will be teared down.
 *
 * @author pmucha
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PactVerification {
    /**
     * the tested provider name.
     */
    String[] value() default "";

    /**
     * Method to call to get the pact fragment. Defaults to empty string which results in using the first one found
     */
    String fragment() default "";
}
