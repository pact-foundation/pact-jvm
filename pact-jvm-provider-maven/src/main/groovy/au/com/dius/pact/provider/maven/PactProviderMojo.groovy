package au.com.dius.pact.provider.maven

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Mojo

@Mojo(name = 'verify')
class PactProviderMojo extends AbstractMojo {

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        println "Hello Maven World"
    }
}
