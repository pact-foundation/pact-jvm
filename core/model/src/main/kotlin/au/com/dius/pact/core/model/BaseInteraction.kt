package au.com.dius.pact.core.model

abstract class BaseInteraction(
  override val interactionId: String? = null,
  override val description: String,
  override val providerStates: List<ProviderState> = listOf()
) : Interaction
