package au.com.dius.pact.provider.junitsupport.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point Pact runner to source of pacts for contract tests
 *
 * @see PactUrlLoader pact loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PactSource(PactUrlLoader.class)
@Inherited
public @interface PactUrl {
    /**
     * @return a list of urls to pact files
     */
    String[] urls();
}
