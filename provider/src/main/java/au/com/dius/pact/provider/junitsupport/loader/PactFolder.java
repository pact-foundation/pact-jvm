package au.com.dius.pact.provider.junitsupport.loader;

import au.com.dius.pact.provider.junitsupport.loader.PactFolderLoader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point Pact runner to source of pacts for contract tests
 *
 * @see PactFolderLoader pact loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PactSource(PactFolderLoader.class)
@Inherited
public @interface PactFolder {
    /**
     * @return path to subfolder of project resource folder with pact
     */
    String value();
}
