package au.com.dius.pact.provider.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

class PactPluginTest {

    private PactPlugin plugin
    private Project project

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build()
        plugin = new PactPlugin()
        plugin.apply(project)
    }

    @Test
    public void 'defines a pactVerify task'() {
        assert project.tasks.pactVerify
    }

    @Test
    public void 'defines a task for each defined provider'() {
        project.extensions.pact.serviceProviders {
            provider1 {

            }

            provider2 {

            }
        }

        project.evaluate()

        assert project.tasks.pactVerify_provider1
        assert project.tasks.pactVerify_provider2
    }

}
