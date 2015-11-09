package au.com.dius.pact.provider.junit.loader;

import au.com.dius.pact.provider.junit.PactRunner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point {@link PactRunner} to source of pacts for contract tests
 *
 * @see PactFolderLoader pact loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PactSource(PactFolderLoader.class)
public @interface PactFolder {
    /**
     * @return path to subfolder of project resource folder with pact
     */
    String value();
}
