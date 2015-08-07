package au.com.dius.pact.provider

import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.Request
import au.com.dius.pact.model.Response

/**
 * Proxy the Scala class to expose it as a POJO
 */
class PactInteractionProxy {

    private final Interaction interaction

    PactInteractionProxy(Interaction interaction) {
        this.interaction = interaction
    }

    String getDescription() {
        interaction.description()
    }

    String getProviderState() {
        interaction.providerState().defined ? interaction.providerState().get() : null
    }

    Request getRequest() {
        interaction.request()
    }

    Response getResponse() {
        interaction.response()
    }
}
