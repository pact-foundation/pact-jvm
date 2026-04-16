package au.com.dius.pact.core.support.regex

import kotlin.random.Random

/**
 * Generates random strings by walking a [RegexNode] expression tree.
 *
 * Every string produced is guaranteed to match the original regular expression
 * (subject to the limitations on lookaheads, lookbehinds, and backreferences,
 * which are treated as zero-width assertions that produce no output).
 *
 * @param maxUnboundedRepetitions upper bound on repetitions for unbounded quantifiers
 *   (`*`, `+`, or `{n,}`). Defaults to [DEFAULT_MAX_UNBOUNDED].
 * @param random source of randomness
 */
class RegexStringGenerator @JvmOverloads constructor(
  private val maxUnboundedRepetitions: Int = DEFAULT_MAX_UNBOUNDED,
  private val random: Random = Random.Default
) {

  /**
   * Generates a random string matching the given [node].
   */
  fun generate(node: RegexNode): String = buildString { appendNode(node) }

  private fun StringBuilder.appendNode(node: RegexNode) {
    when (node) {
      is RegexNode.Literal -> append(node.char)

      is RegexNode.CharClass -> {
        val chars = node.toCharSet()
        if (chars.isNotEmpty()) append(chars[random.nextInt(chars.size)])
      }

      is RegexNode.AnyChar -> {
        // '.' matches any char except newline; use printable ASCII (32–126)
        append(PRINTABLE_ASCII[random.nextInt(PRINTABLE_ASCII.size)])
      }

      is RegexNode.Sequence -> node.nodes.forEach { appendNode(it) }

      is RegexNode.Alternation -> appendNode(node.alternatives[random.nextInt(node.alternatives.size)])

      is RegexNode.Repeat -> repeat(repetitionCount(node)) { appendNode(node.node) }

      is RegexNode.Group -> appendNode(node.node)
    }
  }

  private fun repetitionCount(node: RegexNode.Repeat): Int {
    return when {
      node.min == node.max -> node.min
      node.max == Int.MAX_VALUE -> {
        // Unbounded quantifier: generate between min and max(min, maxUnboundedRepetitions)
        val effectiveMax = maxOf(node.min, maxUnboundedRepetitions)
        if (node.min >= effectiveMax) node.min
        else node.min + random.nextInt(effectiveMax - node.min + 1)
      }
      else -> {
        // Bounded quantifier: stay within [min, max] but cap to avoid very long strings
        val effectiveMax = minOf(node.max, node.min + maxUnboundedRepetitions)
        if (node.min >= effectiveMax) node.min
        else node.min + random.nextInt(effectiveMax - node.min + 1)
      }
    }
  }

  companion object {
    /** Default upper bound on repetitions for unbounded quantifiers. */
    const val DEFAULT_MAX_UNBOUNDED = 5

    private val PRINTABLE_ASCII: List<Char> = (32..126).map { it.toChar() }

    /**
     * Convenience: parses [pattern] and returns a random string matching it.
     *
     * @param pattern               the regular expression pattern
     * @param maxUnboundedRepetitions upper bound for `*`, `+`, `{n,}` repetitions
     * @param random                source of randomness
     */
    @JvmStatic
    @JvmOverloads
    fun generate(
      pattern: String,
      maxUnboundedRepetitions: Int = DEFAULT_MAX_UNBOUNDED,
      random: Random = Random.Default
    ): String = RegexStringGenerator(maxUnboundedRepetitions, random)
      .generate(RegexParser(pattern).parse())
  }
}
