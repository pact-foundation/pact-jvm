package au.com.dius.pact.provider.junitsupport.loader

class KotlinClassWithSelectorMethod {
  @PactBrokerConsumerVersionSelectors
  fun consumerVersionSelectors() = SelectorBuilder().environment("KotlinSelectorMethod")
}

abstract class KotlinAbstractClassWithSelectorMethod {
  @PactBrokerConsumerVersionSelectors
  fun consumerVersionSelectors() = SelectorBuilder().environment("KotlinSelectorMethod")
}

class KotlinClassWithStaticSelectorMethod {
  companion object {
    @PactBrokerConsumerVersionSelectors
    fun consumerVersionSelectors() = SelectorBuilder().environment("KotlinStaticSelectorMethod")
  }
}
