package au.com.dius.pact.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation for a method that will setup the default request values used in the test. The annotated
 * method must take a single parameter of PactDslRequestWithoutPath and set the default values on that object
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DefaultRequestValues {
}
