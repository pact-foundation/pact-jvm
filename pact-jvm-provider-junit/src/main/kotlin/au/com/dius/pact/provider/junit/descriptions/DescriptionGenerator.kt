package au.com.dius.pact.provider.junit.descriptions

import au.com.dius.pact.core.model.BrokerUrlSource
import au.com.dius.pact.core.model.Interaction
import au.com.dius.pact.core.model.Pact
import au.com.dius.pact.core.model.PactSource
import au.com.dius.pact.core.support.isNotEmpty
import org.junit.runner.Description
import org.junit.runners.model.TestClass

/**
 * Class responsible for building junit tests Description.
 */
class DescriptionGenerator<I : Interaction>(private val testClass: TestClass,
                                            private val pact: Pact<I>,
                                            private val pactSource: PactSource) {

    /**
     * Builds an instance of junit Description adhering with this logic for building the name:
     * If the PactSource is of type <code>BrokerUrlSource</code> and its tag is not empty then
     * the test name will be "[tag:#tagname] #consumername - Upon #interaction".
     * For all the other cases "#consumername - Upon #interaction"
     * @param interaction the Interaction under test
     */
    fun generate(interaction: Interaction): Description {
        return Description.createTestDescription(testClass.javaClass,
                "${this.getNamePrefix()}${pact.consumer.name} - Upon ${interaction.description}")
    }

    private fun getNamePrefix(): String {
        if (pactSource is BrokerUrlSource && pactSource.tag.isNotEmpty()) {
            return "[tag:${pactSource.tag}] "
        }
        return ""
    }
}