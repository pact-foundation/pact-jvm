package au.com.dius.pact.core.support.regex

/**
 * Sealed class hierarchy representing an expression tree for a parsed regular expression.
 * Each node corresponds to one structural element of the pattern.
 */
sealed class RegexNode {

  /** A single literal character, e.g. 'a', '5', '@'. */
  data class Literal(val char: Char) : RegexNode()

  /**
   * A character class, e.g. `[a-z0-9]`, `[^abc]`, `\w`, `\d`, `\s`.
   *
   * @param ranges  list of inclusive character ranges, each as a (from, to) pair
   * @param chars   individual characters that belong directly to the class
   * @param negated true if this is a negated class — a matching character must NOT be in the class
   */
  data class CharClass(
    val ranges: List<Pair<Char, Char>>,
    val chars: List<Char>,
    val negated: Boolean = false
  ) : RegexNode() {

    /**
     * Returns all characters in this class within printable ASCII (code points 32–126).
     * For negated classes the complement within that range is returned.
     */
    fun toCharSet(): List<Char> {
      val included = mutableListOf<Char>()
      for ((from, to) in ranges) {
        for (c in from..to) included.add(c)
      }
      included.addAll(chars)
      return if (negated) {
        val includedSet = included.toHashSet()
        PRINTABLE_ASCII.filter { it !in includedSet }
      } else {
        included.distinct()
      }
    }

    companion object {
      /** All printable ASCII characters (code points 32–126 inclusive). */
      val PRINTABLE_ASCII: List<Char> = (32..126).map { it.toChar() }
    }
  }

  /** Matches any single character, equivalent to `.` in a regex (not matching newline). */
  object AnyChar : RegexNode()

  /**
   * A sequence (concatenation) of nodes. An empty list represents the empty string.
   *
   * @param nodes the ordered list of nodes to concatenate
   */
  data class Sequence(val nodes: List<RegexNode>) : RegexNode()

  /**
   * An alternation (`a|b|c`) — one alternative is chosen at random during generation.
   *
   * @param alternatives the list of alternatives; must have at least two entries
   */
  data class Alternation(val alternatives: List<RegexNode>) : RegexNode()

  /**
   * Repetition of a node.
   *
   * @param node the repeated node
   * @param min  minimum number of repetitions (0 or greater)
   * @param max  maximum number of repetitions; [Int.MAX_VALUE] indicates unbounded (`*`, `+`)
   */
  data class Repeat(val node: RegexNode, val min: Int, val max: Int) : RegexNode()

  /**
   * A (capturing or non-capturing) group. The outer parentheses do not affect generation.
   *
   * @param node the inner expression
   */
  data class Group(val node: RegexNode) : RegexNode()
}
