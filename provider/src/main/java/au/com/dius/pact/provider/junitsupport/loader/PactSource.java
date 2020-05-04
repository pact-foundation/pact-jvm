package au.com.dius.pact.provider.junitsupport.loader;

import au.com.dius.pact.provider.junitsupport.loader.PactBrokerLoader;
import au.com.dius.pact.provider.junitsupport.loader.PactFolderLoader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Point out which {@link PactLoader} use for pact loading
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Inherited
public @interface PactSource {
    /**
     * @return {@link PactLoader} class that will be used for pact loading
     *
     * @see PactLoader
     * @see PactBrokerLoader loads pacts from Pact broker
     * @see PactFolderLoader loads pacts from folder
     * @see PactUrlLoader loads pacts from urls
     */
    Class<? extends PactLoader> value();
}
