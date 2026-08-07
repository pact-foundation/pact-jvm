package au.com.dius.pact.core.model.plugins

import au.com.dius.pact.core.model.generators.GeneratorTestMode
import au.com.dius.pact.core.support.json.JsonValue

/**
 * Support for plugin-provided matching rules and generators, supplied by the host framework.
 *
 * Resolving a plugin-provided rule or generator means looking its name up in the plugin catalogue,
 * which lives in the plugin driver - and this module does not depend on the driver. So the two
 * places in the model that need to reach the catalogue go through a handler registered here:
 *
 *  * applying a [au.com.dius.pact.core.model.generators.PluginGenerator], since generators are
 *    applied from the model;
 *  * resolving the `config-key` of a plugin rule while parsing a matching rule definition
 *    expression.
 *
 * `core:matchers` registers the implementation in `MatchingConfig.registerCoreCapabilities()`.
 * With none registered, both fail with an error telling the user to load the plugin.
 *
 * See proposal 006, Field-level matchers and generators.
 */
interface PluginSupport {
  /**
   * The values key that a single positional configuration argument in
   * `matching(NAME, CONFIG, EXAMPLE)` maps to. This is the `config-key` value on the rule's
   * catalogue entry; null means the entry does not set one, and the caller uses `value`.
   */
  fun configKey(ruleName: String): String?

  /**
   * Apply a plugin-provided generator to a single value, returning the generated value.
   *
   * @param path Path to the value being generated, as a Pact matching rule expression. The
   *   generator application path does not always know one, in which case this is the document root.
   * @param mode Which side of the test the generator is running on, or null where the generator
   *   application path does not know. A plugin generator that behaves differently per side (the
   *   way `ProviderState` and `MockServerURL` do) needs this; passing null means "apply it
   *   regardless" rather than guessing a side.
   */
  // Every one of these is data the plugin interface's GenerateFieldRequest carries in its own
  // right, so bundling them into a holder here would only move the parameter list one call further
  // out
  @Suppress("LongParameterList")
  fun generate(
    name: String,
    values: Map<String, JsonValue>,
    exampleValue: Any?,
    path: String,
    mode: GeneratorTestMode?,
    context: Map<String, Any>
  ): Any?
}

/**
 * The registered [PluginSupport] handler. See that interface for why this exists.
 */
object PluginSupportRegistry {
  @Volatile
  private var support: PluginSupport? = null

  /** Registers the handler for plugin-provided matching rules and generators */
  @JvmStatic
  fun register(support: PluginSupport) {
    this.support = support
  }

  /** The registered handler, if the host has set one up */
  @JvmStatic
  fun support(): PluginSupport? = support

  /**
   * The values key a single positional configuration argument for the given plugin rule maps to,
   * defaulting to `value` when there is no handler or the catalogue entry does not set a
   * `config-key`.
   */
  @JvmStatic
  fun configKeyFor(ruleName: String): String = support?.configKey(ruleName) ?: "value"
}
