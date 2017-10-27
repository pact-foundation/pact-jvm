package au.com.dius.pact.provider.junit.loader;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to point {@link au.com.dius.pact.provider.junit.PactRunner} to a versioned source of pacts for contract tests.
 * <p>
 *     Use ${any.variable} in the url and specify any.variable as a system property.
 * </p>
 * <p>
 *     For example, when you annotate a provider test class with:
 * <pre><code>{@literal @}VersionedPactUrl(urls = {"http://artifactory:8081/artifactory/consumercontracts/foo-bar/${foo.version}/foo-bar-${foo.version}.json"})</code></pre>
 * And pass a system property foo.version to the JVM, for example -Dfoo.version=123
 * <p>
 * Then the pact tests will fetch the following contract:
 * <pre><code>http://artifactory:8081/artifactory/consumercontracts/foo-bar/123/foo-bar-123.json</code></pre>
 *
 * @see VersionedPactUrlLoader pact loader
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@PactSource(VersionedPactUrlLoader.class)
public @interface VersionedPactUrl {
    /**
     * @return a list of urls to pact files
     */
    String[] urls();
}
