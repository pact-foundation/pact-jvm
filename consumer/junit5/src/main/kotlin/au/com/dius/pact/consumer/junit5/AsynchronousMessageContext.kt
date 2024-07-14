package au.com.dius.pact.consumer.junit5

import au.com.dius.pact.core.model.V4Interaction
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

class AsynchronousMessageContext(
    val message: V4Interaction.AsynchronousMessage
): TestTemplateInvocationContext {
}