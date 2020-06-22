package au.com.dius.pact.provider.junitsupport.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to specify which versions to use when querying the Pact matrix.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface VersionSelector {
    /**
     * @return Tags to use to fetch pacts for, defaults to `latest`
     */
    String tag() default "latest";

    /**
     * @return "true" to fetch the latest version of the pact, or "false" to fetch all versions
     */
    String latest() default "true";
}
