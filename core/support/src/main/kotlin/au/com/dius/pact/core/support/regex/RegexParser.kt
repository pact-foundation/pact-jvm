package au.com.dius.pact.core.support.regex

/**
 * Recursive-descent parser for Java/Kotlin regular expressions.
 * Parses a regex pattern string into a [RegexNode] expression tree.
 *
 * Supported constructs:
 * - Literal characters and escape sequences: `\n`, `\t`, `\r`, `\f`, `\0`, `\xHH`, `\uHHHH`
 * - Escaped metacharacters: `\.`, `\*`, `\(`, `\[`, `\\`, etc.
 * - Predefined character classes: `\w`, `\W`, `\d`, `\D`, `\s`, `\S`
 * - Unicode property classes: `\p{Alpha}`, `\p{Digit}`, `\p{Upper}`, `\p{Lower}`,
 *   `\p{Alnum}`, `\p{Space}`, `\p{XDigit}`, and single-letter variants (`\pL`, `\pN`)
 * - Negated Unicode property classes: `\P{...}`
 * - Custom character classes: `[abc]`, `[a-z]`, `[^abc]`, with nested escape sequences
 *   and POSIX-style classes like `[[:alpha:]]`
 * - Any-character `.`
 * - Anchors `^` and `$` — consumed but produce no output during generation
 * - Groups: `(…)`, `(?:…)`, `(?<name>…)`, `(?=…)`, `(?!…)`, `(?<=…)`, `(?<!…)`,
 *   inline-flag groups like `(?i)` and `(?i:…)` (lookahead/lookbehind produce no output)
 * - Alternation `a|b|c`
 * - Quantifiers `*`, `+`, `?`, `{n}`, `{n,}`, `{n,m}`, with optional lazy `?` or possessive `+`
 * - Word boundaries `\b`, `\B` — consumed but produce no output
 * - Backreferences `\1`–`\9` — consumed but produce no output
 */
@Suppress("TooManyFunctions")
class RegexParser(private val pattern: String) {
  private var pos = 0

  /**
   * Parses the full pattern and returns the root [RegexNode] of the expression tree.
   */
  fun parse(): RegexNode = parseAlternation()

  // ── Grammar productions ──────────────────────────────────────────────────

  private fun parseAlternation(): RegexNode {
    val alts = mutableListOf(parseSequence())
    while (pos < pattern.length && pattern[pos] == '|') {
      pos++ // consume '|'
      alts.add(parseSequence())
    }
    return if (alts.size == 1) alts[0] else RegexNode.Alternation(alts)
  }

  private fun parseSequence(): RegexNode {
    val nodes = mutableListOf<RegexNode>()
    while (pos < pattern.length && pattern[pos] != ')' && pattern[pos] != '|') {
      nodes.add(parseTerm())
    }
    return when (nodes.size) {
      0 -> RegexNode.Sequence(emptyList())
      1 -> nodes[0]
      else -> RegexNode.Sequence(nodes)
    }
  }

  private fun parseTerm(): RegexNode = parseQuantifier(parseAtom())

  private fun parseAtom(): RegexNode {
    if (pos >= pattern.length) return RegexNode.Sequence(emptyList())
    return when (pattern[pos]) {
      '(' -> parseGroup()
      '[' -> parseCharClass()
      '.' -> { pos++; RegexNode.AnyChar }
      '\\' -> parseEscape()
      '^', '$' -> { pos++; RegexNode.Sequence(emptyList()) } // anchors → no output
      else -> RegexNode.Literal(pattern[pos++])
    }
  }

  // ── Group ────────────────────────────────────────────────────────────────

  @Suppress("CyclomaticComplexMethod")
  private fun parseGroup(): RegexNode {
    pos++ // consume '('
    var ignore = false

    if (pos < pattern.length && pattern[pos] == '?') {
      pos++ // consume '?'
      when {
        pos < pattern.length && pattern[pos] == ':' -> pos++ // non-capturing (?:...)
        pos < pattern.length && pattern[pos] == '>' -> pos++ // atomic group (?>...)
        pos < pattern.length && (pattern[pos] == '=' || pattern[pos] == '!') -> {
          pos++ // lookahead (?= or (?! — zero-width, produces no output
          ignore = true
        }
        pos < pattern.length && pattern[pos] == '<' -> {
          pos++ // consume '<'
          when {
            pos < pattern.length && (pattern[pos] == '=' || pattern[pos] == '!') -> {
              pos++ // lookbehind (?<= or (?<! — zero-width, produces no output
              ignore = true
            }
            else -> {
              // Named group (?<name>...): skip the name
              while (pos < pattern.length && pattern[pos] != '>') pos++
              if (pos < pattern.length) pos++ // consume '>'
            }
          }
        }
        else -> {
          // Inline-flag group like (?i), (?m), or (?i:...): skip flag characters
          while (pos < pattern.length && pattern[pos] != ':' && pattern[pos] != ')') pos++
          if (pos < pattern.length && pattern[pos] == ':') pos++ // (?flags:...) form
        }
      }
    }

    val inner = parseAlternation()
    if (pos < pattern.length && pattern[pos] == ')') pos++ // consume ')'

    return if (ignore) RegexNode.Sequence(emptyList()) else RegexNode.Group(inner)
  }

  // ── Character class ──────────────────────────────────────────────────────

  @Suppress("CyclomaticComplexMethod")
  private fun parseCharClass(): RegexNode {
    pos++ // consume '['
    val negated = pos < pattern.length && pattern[pos] == '^'
    if (negated) pos++

    val ranges = mutableListOf<Pair<Char, Char>>()
    val chars = mutableListOf<Char>()

    // ']' immediately after '[' or '[^' is a literal ']', not the closing bracket
    if (pos < pattern.length && pattern[pos] == ']') {
      chars.add(']')
      pos++
    }

    while (pos < pattern.length && pattern[pos] != ']') {
      when {
        pattern[pos] == '\\' -> {
          pos++ // consume '\\'
          val (r, c) = parseCharClassEscape()
          ranges.addAll(r)
          chars.addAll(c)
        }
        // POSIX-style class like [[:alpha:]]
        pattern[pos] == '[' && pos + 1 < pattern.length && pattern[pos + 1] == ':' -> {
          pos += 2 // consume '[:'
          val name = buildString {
            while (pos < pattern.length && pattern[pos] != ':') append(pattern[pos++])
          }
          if (pos + 1 < pattern.length && pattern[pos] == ':' && pattern[pos + 1] == ']') pos += 2
          val (r, c) = expandPosixClass(name)
          ranges.addAll(r)
          chars.addAll(c)
        }
        else -> {
          val startChar = pattern[pos++]
          // Range x-y: '-' must not be the last char before ']'
          if (pos + 1 < pattern.length && pattern[pos] == '-' && pattern[pos + 1] != ']') {
            pos++ // consume '-'
            val endChar = when {
              pos < pattern.length && pattern[pos] == '\\' -> { pos++; parseSingleEscapeChar() }
              pos < pattern.length -> pattern[pos++]
              else -> startChar
            }
            if (startChar <= endChar) {
              ranges.add(Pair(startChar, endChar))
            } else {
              // Inverted range — not valid; add as individual characters
              chars.add(startChar)
              chars.add('-')
              chars.add(endChar)
            }
          } else {
            chars.add(startChar)
          }
        }
      }
    }

    if (pos < pattern.length) pos++ // consume ']'
    return RegexNode.CharClass(ranges, chars, negated)
  }

  /**
   * Parses the character(s) that follow a `\` inside a character class.
   * Returns (ranges, chars) for what the escape contributes to the class.
   * Called after the backslash has already been consumed.
   */
  @Suppress("CyclomaticComplexMethod")
  private fun parseCharClassEscape(): Pair<List<Pair<Char, Char>>, List<Char>> {
    if (pos >= pattern.length) return Pair(emptyList(), emptyList())
    return when (val ch = pattern[pos++]) {
      'w' -> Pair(listOf(Pair('a', 'z'), Pair('A', 'Z'), Pair('0', '9')), listOf('_'))
      'W' -> Pair(listOf(Pair(' ', '/'), Pair(':', '@'), Pair('[', '^'), Pair('{', '~')), listOf('`'))
      'd' -> Pair(listOf(Pair('0', '9')), emptyList())
      'D' -> Pair(listOf(Pair(' ', '/'), Pair(':', '~')), emptyList())
      's' -> Pair(emptyList(), listOf(' ', '\t', '\n', '\u000B', '\r', '\u000C'))
      'S' -> Pair(listOf(Pair('!', '~')), emptyList())
      'p' -> parseUnicodePropertyRanges()
      'P' -> parseUnicodePropertyRanges() // negation handled at CharClass level
      'n' -> Pair(emptyList(), listOf('\n'))
      't' -> Pair(emptyList(), listOf('\t'))
      'r' -> Pair(emptyList(), listOf('\r'))
      'f' -> Pair(emptyList(), listOf('\u000C'))
      '0' -> Pair(emptyList(), listOf('\u0000'))
      'x' -> Pair(emptyList(), listOf(parseHexEscape()))
      'u' -> Pair(emptyList(), listOf(parseUnicodeEscape()))
      else -> Pair(emptyList(), listOf(ch))
    }
  }

  /**
   * Parses a single escape character that appears in a char-class range (e.g. `\t` in `\t-\n`).
   * Called after the backslash has already been consumed.
   */
  private fun parseSingleEscapeChar(): Char {
    if (pos >= pattern.length) return '\\'
    return when (val ch = pattern[pos++]) {
      'n' -> '\n'
      't' -> '\t'
      'r' -> '\r'
      'f' -> '\u000C'
      '0' -> '\u0000'
      'x' -> parseHexEscape()
      'u' -> parseUnicodeEscape()
      else -> ch
    }
  }

  // ── Escape sequences ─────────────────────────────────────────────────────

  @Suppress("CyclomaticComplexMethod")
  private fun parseEscape(): RegexNode {
    pos++ // consume '\\'
    require(pos < pattern.length) { "Trailing backslash in regex pattern" }
    return when (val ch = pattern[pos++]) {
      'w' -> RegexNode.CharClass(listOf(Pair('a', 'z'), Pair('A', 'Z'), Pair('0', '9')), listOf('_'))
      'W' -> RegexNode.CharClass(listOf(Pair(' ', '/'), Pair(':', '@'), Pair('[', '^'), Pair('{', '~')), listOf('`'))
      'd' -> RegexNode.CharClass(listOf(Pair('0', '9')), emptyList())
      'D' -> RegexNode.CharClass(listOf(Pair(' ', '/'), Pair(':', '~')), emptyList())
      's' -> RegexNode.CharClass(emptyList(), listOf(' ', '\t', '\n', '\u000B', '\r', '\u000C'))
      'S' -> RegexNode.CharClass(listOf(Pair('!', '~')), emptyList())
      'n' -> RegexNode.Literal('\n')
      't' -> RegexNode.Literal('\t')
      'r' -> RegexNode.Literal('\r')
      'f' -> RegexNode.Literal('\u000C')
      '0' -> RegexNode.Literal('\u0000')
      'b', 'B' -> RegexNode.Sequence(emptyList()) // word boundary — no output
      'p' -> {
        val (r, c) = parseUnicodePropertyRanges()
        RegexNode.CharClass(r, c)
      }
      'P' -> {
        val (r, c) = parseUnicodePropertyRanges()
        RegexNode.CharClass(r, c, negated = true)
      }
      'x' -> RegexNode.Literal(parseHexEscape())
      'u' -> RegexNode.Literal(parseUnicodeEscape())
      in '1'..'9' -> RegexNode.Sequence(emptyList()) // backreference — no output
      else -> RegexNode.Literal(ch) // escaped metachar or literal
    }
  }

  /** Parses a `\xHH` or `\x{HHHH}` hex escape. Called after `x` has been consumed. */
  private fun parseHexEscape(): Char {
    return if (pos < pattern.length && pattern[pos] == '{') {
      pos++ // consume '{'
      val hex = buildString { while (pos < pattern.length && pattern[pos] != '}') append(pattern[pos++]) }
      if (pos < pattern.length) pos++ // consume '}'
      hex.toInt(16).toChar()
    } else {
      val end = minOf(pos + 2, pattern.length)
      val hex = pattern.substring(pos, end)
      pos = end
      hex.toInt(16).toChar()
    }
  }

  /** Parses a `\uHHHH` or `\u{HHHH}` Unicode escape. Called after `u` has been consumed. */
  private fun parseUnicodeEscape(): Char {
    return if (pos < pattern.length && pattern[pos] == '{') {
      pos++ // consume '{'
      val hex = buildString { while (pos < pattern.length && pattern[pos] != '}') append(pattern[pos++]) }
      if (pos < pattern.length) pos++ // consume '}'
      hex.toInt(16).toChar()
    } else {
      val end = minOf(pos + 4, pattern.length)
      val hex = pattern.substring(pos, end)
      pos = end
      hex.toInt(16).toChar()
    }
  }

  // ── Unicode / POSIX property classes ────────────────────────────────────

  /**
   * Parses `{Name}` or a single letter after `\p` / `\P`.
   * Returns (ranges, chars) for the printable-ASCII equivalent of the property.
   * Called after `p` or `P` has been consumed.
   */
  private fun parseUnicodePropertyRanges(): Pair<List<Pair<Char, Char>>, List<Char>> {
    val name = if (pos < pattern.length && pattern[pos] == '{') {
      pos++ // consume '{'
      val n = buildString { while (pos < pattern.length && pattern[pos] != '}') append(pattern[pos++]) }
      if (pos < pattern.length) pos++ // consume '}'
      n
    } else if (pos < pattern.length) {
      pattern[pos++].toString()
    } else {
      "L"
    }
    return expandUnicodeProperty(name)
  }

  private fun expandUnicodeProperty(name: String): Pair<List<Pair<Char, Char>>, List<Char>> {
    return when (name.lowercase()) {
      "l", "letter", "alpha", "is_alpha", "javaletterordigit" ->
        Pair(listOf(Pair('a', 'z'), Pair('A', 'Z')), emptyList())
      "lu", "uppercase_letter", "upper", "is_upper", "javauppercase" ->
        Pair(listOf(Pair('A', 'Z')), emptyList())
      "ll", "lowercase_letter", "lower", "is_lower", "javalowercase" ->
        Pair(listOf(Pair('a', 'z')), emptyList())
      "n", "nd", "digit", "is_digit", "javadigit" ->
        Pair(listOf(Pair('0', '9')), emptyList())
      "alnum", "alphanumeric" ->
        Pair(listOf(Pair('a', 'z'), Pair('A', 'Z'), Pair('0', '9')), emptyList())
      "word" ->
        Pair(listOf(Pair('a', 'z'), Pair('A', 'Z'), Pair('0', '9')), listOf('_'))
      "s", "z", "zs", "space", "whitespace", "javawhitespace" ->
        Pair(emptyList(), listOf(' ', '\t', '\n', '\u000B', '\r', '\u000C'))
      "p", "po", "punct" ->
        Pair(listOf(Pair('!', '/'), Pair(':', '@'), Pair('[', '`'), Pair('{', '~')), emptyList())
      "ascii", "is_ascii" ->
        Pair(listOf(Pair(' ', '~')), emptyList())
      "xdigit", "hex_digit" ->
        Pair(listOf(Pair('0', '9'), Pair('a', 'f'), Pair('A', 'F')), emptyList())
      "print", "graph" ->
        Pair(listOf(Pair(' ', '~')), emptyList())
      else ->
        Pair(listOf(Pair('a', 'z'), Pair('A', 'Z'), Pair('0', '9')), emptyList())
    }
  }

  private fun expandPosixClass(name: String): Pair<List<Pair<Char, Char>>, List<Char>> {
    return when (name.lowercase()) {
      "alpha" -> Pair(listOf(Pair('a', 'z'), Pair('A', 'Z')), emptyList())
      "digit" -> Pair(listOf(Pair('0', '9')), emptyList())
      "alnum" -> Pair(listOf(Pair('a', 'z'), Pair('A', 'Z'), Pair('0', '9')), emptyList())
      "space" -> Pair(emptyList(), listOf(' ', '\t', '\n', '\u000B', '\r', '\u000C'))
      "blank" -> Pair(emptyList(), listOf(' ', '\t'))
      "upper" -> Pair(listOf(Pair('A', 'Z')), emptyList())
      "lower" -> Pair(listOf(Pair('a', 'z')), emptyList())
      "punct" -> Pair(listOf(Pair('!', '/'), Pair(':', '@'), Pair('[', '`'), Pair('{', '~')), emptyList())
      "xdigit" -> Pair(listOf(Pair('0', '9'), Pair('a', 'f'), Pair('A', 'F')), emptyList())
      "print", "graph" -> Pair(listOf(Pair(' ', '~')), emptyList())
      else -> Pair(listOf(Pair('a', 'z'), Pair('A', 'Z')), emptyList())
    }
  }

  // ── Quantifier ───────────────────────────────────────────────────────────

  @Suppress("ReturnCount", "CyclomaticComplexMethod", "NestedBlockDepth")
  private fun parseQuantifier(node: RegexNode): RegexNode {
    if (pos >= pattern.length) return node
    val quantified = when (pattern[pos]) {
      '*' -> { pos++; RegexNode.Repeat(node, 0, Int.MAX_VALUE) }
      '+' -> { pos++; RegexNode.Repeat(node, 1, Int.MAX_VALUE) }
      '?' -> { pos++; RegexNode.Repeat(node, 0, 1) }
      '{' -> {
        val savedPos = pos
        pos++ // consume '{'
        if (pos < pattern.length && pattern[pos].isDigit()) {
          val min = parseNumber()
          when {
            pos < pattern.length && pattern[pos] == '}' -> {
              pos++ // consume '}'
              RegexNode.Repeat(node, min, min)
            }
            pos < pattern.length && pattern[pos] == ',' -> {
              pos++ // consume ','
              when {
                pos < pattern.length && pattern[pos] == '}' -> {
                  pos++ // consume '}'
                  RegexNode.Repeat(node, min, Int.MAX_VALUE)
                }
                pos < pattern.length && pattern[pos].isDigit() -> {
                  val max = parseNumber()
                  if (pos < pattern.length && pattern[pos] == '}') {
                    pos++ // consume '}'
                    if (max >= min) RegexNode.Repeat(node, min, max)
                    else { pos = savedPos; return node }
                  } else { pos = savedPos; return node }
                }
                else -> { pos = savedPos; return node }
              }
            }
            else -> { pos = savedPos; return node }
          }
        } else {
          pos = savedPos
          return node
        }
      }
      else -> return node
    }

    // Consume optional lazy '?' or possessive '+' modifier — no effect on generation
    if (pos < pattern.length && (pattern[pos] == '?' || pattern[pos] == '+')) pos++

    return quantified
  }

  private fun parseNumber(): Int {
    var n = 0
    while (pos < pattern.length && pattern[pos].isDigit()) {
      n = n * 10 + pattern[pos++].digitToInt()
    }
    return n
  }
}
