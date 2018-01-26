package au.com.dius.pact.provider.junit.target;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark {@link au.com.dius.pact.provider.junit.target.Target} for contract tests
 *
 * @see au.com.dius.pact.provider.junit.target.Target
 * @see HttpTarget
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface TestTarget {
}
