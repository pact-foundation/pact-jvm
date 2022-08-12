package au.com.dius.pact.provider.junitsupport.loader

class KotlinClassWithSelectorMethod {
  @PactBrokerConsumerVersionSelectors
  fun consumerVersionSelectors() = SelectorBuilder().environment("KotlinSelectorMethod")
}

@Suppress("UnnecessaryAbstractClass")
abstract class KotlinAbstractClassWithSelectorMethod {
  @PactBrokerConsumerVersionSelectors
  fun consumerVersionSelectors() = SelectorBuilder().environment("KotlinSelectorMethod")
}

@Suppress("UtilityClassWithPublicConstructor")
class KotlinClassWithStaticSelectorMethod {
  companion object {
    @PactBrokerConsumerVersionSelectors
    fun consumerVersionSelectors() = SelectorBuilder().environment("KotlinStaticSelectorMethod")
  }
}
