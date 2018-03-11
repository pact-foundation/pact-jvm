package au.com.dius.pact.model

enum class Features(val featureKey: String) {
  UseMatchValuesMatcher("pact.feature.matchers.useMatchValuesMatcher")
}

object FeatureToggles {

  private var features = mutableMapOf<String, Any>()

  init {
    reset()
  }

  @JvmStatic
  fun toggleFeature(name: String, value: Boolean) {
    features[name] = value
  }

  @JvmStatic
  fun isFeatureSet(name: String) =
    features[name] != null && features[name] is Boolean && features[name] as Boolean

  @JvmStatic
  fun reset() {
    features = mutableMapOf(Features.UseMatchValuesMatcher.featureKey to false)
  }

  @JvmStatic
  fun features() = features.toMap()
}
