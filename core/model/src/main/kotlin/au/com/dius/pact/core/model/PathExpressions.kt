package au.com.dius.pact.core.model

import org.apache.commons.collections4.iterators.PushbackIterator
import org.apache.commons.lang3.StringUtils

const val PATH_SPECIAL_CHARS = "'[].@ \t\n"
const val EXP_ALLOWED_SPECIAL_CHARS = "-_:#@"

sealed class PathToken {
  abstract fun rawString(): String

  object Root : PathToken() {
    override fun toString() = "$"

    override fun rawString() = "$"
  }

  data class Field(val name: String) : PathToken() {
    override fun toString(): String {
      return if (StringUtils.containsAny(this.name, PATH_SPECIAL_CHARS)) {
        "['${this.name}']"
      } else {
        this.name
      }
    }

    override fun rawString() = this.name
  }

  data class Index(val index: Int) : PathToken() {
    override fun toString(): String {
      return "[${this.index}]"
    }

    override fun rawString() = this.index.toString()
  }

  object Star : PathToken() {
    override fun toString() = "*"

    override fun rawString() = "*"
  }

  object StarIndex : PathToken() {
    override fun toString() = "[*]"

    override fun rawString() = "[*]"
  }
}

// string_path -> [^']+
fun stringPath(chars: PushbackIterator<IndexedValue<Char>>, tokens: MutableList<PathToken>, path: String, index: Int) {
  var id = String()
  var c: IndexedValue<Char> = IndexedValue(index, ' ')

  while (c.value != '\'' && chars.hasNext()) {
    c = chars.next()

    if (c.value == '\'') {
      if (id.isEmpty()) {
        throw InvalidPathExpression("Empty strings are not allowed in path expression \"$path\" at index ${c.index}")
      } else {
        break
      }
    } else {
      id += c.value
    }
  }

  if (c.value == '\'') {
    tokens.add(PathToken.Field(id))
  } else {
    throw InvalidPathExpression("Unterminated string in path expression \"$path\" at index ${c.index}")
  }
}

// index_path -> [0-9]+
fun indexPath(
  ch: IndexedValue<Char>,
  chars: PushbackIterator<IndexedValue<Char>>,
  tokens: MutableList<PathToken>,
  path: String
) {
  var id = String() + ch.value
  loop@ while (chars.hasNext()) {
    val c = chars.next()
    when {
      c.value.isDigit() -> id += c.value
      c.value == ']' -> {
        chars.pushback(c)
        break@loop
      }
      else -> throw InvalidPathExpression("Indexes can only consist of numbers or a \"*\", found \"${c.value}\" " +
        "instead in path expression \"$path\" at index ${c.index}")
    }
  }

  tokens.add(PathToken.Index(id.toInt()))
}

// identifier -> a-zA-Z0-9\-:+
fun identifier(ch: Char, chars: PushbackIterator<IndexedValue<Char>>, tokens: MutableList<PathToken>, path: String) {
  var id = String() + ch
  while (chars.hasNext()) {
    val c = chars.next()
    if (validPathCharacter(c.value)) {
      id += c.value
    } else if (c.value == '.' || c.value == '\'' || c.value == '[') {
      chars.pushback(c)
      break
    } else {
      throw InvalidPathExpression("\"${c.value}\" is not allowed in an identifier in path expression \"$path\"" +
        " at index ${c.index}")
    }
  }
  tokens.add(PathToken.Field(id))
}

// path_identifier -> identifier | *
fun pathIdentifier(
  chars: PushbackIterator<IndexedValue<Char>>,
  tokens: MutableList<PathToken>,
  path: String,
  index: Int
) {
  if (chars.hasNext()) {
    val ch = chars.next()
    when {
      ch.value == '*' -> tokens.add(PathToken.Star)
      validPathCharacter(ch.value) ->
        identifier(ch.value, chars, tokens, path)
      else -> throw InvalidPathExpression("Expected either a \"*\" or path identifier in path expression \"$path\"" +
        " at index ${ch.index}")
    }
  } else {
    throw InvalidPathExpression("Expected a path after \".\" in path expression \"$path\" at index $index")
  }
}

fun validPathCharacter(c: Char) = c.isLetterOrDigit() || EXP_ALLOWED_SPECIAL_CHARS.contains(c)

// bracket_path -> (string_path | index | *) ]
@Suppress("ThrowsCount")
fun bracketPath(chars: PushbackIterator<IndexedValue<Char>>, tokens: MutableList<PathToken>, path: String, index: Int) {
  if (chars.hasNext()) {
    val ch = chars.next()
    when {
      ch.value == '\'' -> stringPath(chars, tokens, path, ch.index)
      ch.value.isDigit() -> indexPath(ch, chars, tokens, path)
      ch.value == '*' -> tokens.add(PathToken.StarIndex)
      ch.value == ']' -> throw InvalidPathExpression("Empty bracket expressions are not allowed in path expression " +
        "\"$path\" at index ${ch.index}")
      else -> throw InvalidPathExpression("Indexes can only consist of numbers or a \"*\", found \"${ch.value}\" " +
        "instead in path expression \"$path\" at index ${ch.index}")
    }
    if (chars.hasNext()) {
      val c = chars.next()
      if (c.value != ']') {
        throw InvalidPathExpression("Unterminated brackets, found \"${c.value}\" instead of \"]\" " +
          "in path expression \"$path\" at index ${c.index}")
      }
    } else {
      throw InvalidPathExpression("Unterminated brackets in path expression \"$path\" at index ${ch.index}")
    }
  } else {
    throw InvalidPathExpression("Expected a \"'\" (single quote) or a digit in path expression \"$path\"" +
      " after index $index")
  }
}

// path_exp -> (dot-path | bracket-path)*
fun pathExp(chars: PushbackIterator<IndexedValue<Char>>, tokens: MutableList<PathToken>, path: String) {
  while (chars.hasNext()) {
    val next = chars.next()
    when (next.value) {
      '.' -> pathIdentifier(chars, tokens, path, next.index)
      '[' -> bracketPath(chars, tokens, path, next.index)
      else -> throw InvalidPathExpression("Expected a \".\" or \"[\" instead of \"${next.value}\" in path expression " +
        "\"$path\" at index ${next.index}")
    }
  }
}

fun parsePath(path: String): List<PathToken> {
  val tokens = ArrayList<PathToken>()

  // parse_path_exp -> $ path_exp | empty
  val chars = PushbackIterator(path.iterator().withIndex())
  if (chars.hasNext()) {
    val ch = chars.next()
    if (ch.value == '$') {
      tokens.add(PathToken.Root)
      pathExp(chars, tokens, path)
    } else {
      throw InvalidPathExpression("Path expression \"$path\" does not start with a root marker \"$\"")
    }
  }

  return tokens
}

/**
 * This will combine the root path and the path segment to make a valid resulting path
 */
fun constructValidPath(segment: String, rootPath: String, numbersAreIndices: Boolean = true): String {
  return when {
    rootPath.isEmpty() -> segment
    segment.isEmpty() -> rootPath
    else -> {
      val root = StringUtils.stripEnd(rootPath, ".")
      if (numbersAreIndices && segment.all { it.isDigit() }) {
        "$root[$segment]"
      } else if (segment != "*" && segment.any { !validPathCharacter(it) }) {
        "$root['$segment']"
      } else {
        "$root.$segment"
      }
    }
  }
}

/**
 * This will combine the list of segments to make a valid path
 */
fun constructPath(path: List<String>) =
  path.fold("") { path, segment ->
    if (path.isEmpty()) {
      segment
    } else {
      constructValidPath(segment, path)
    }
  }

/**
 * This will combine the path tokens into a valid path
 */
fun pathFromTokens(tokens: List<PathToken>): String {
  return tokens.fold("") { acc, token ->
    acc + when (token) {
      PathToken.Root -> "$"
      is PathToken.Field -> {
        val s = token.toString()
        if (acc.isEmpty() || s.startsWith("[")) {
          s
        } else {
          ".$s"
        }
      }
      is PathToken.Index -> "[${token.index}]"
      PathToken.Star -> if (acc.isEmpty()) "*" else ".*"
      PathToken.StarIndex -> "[*]"
    }
  }
}
