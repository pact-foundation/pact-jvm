package au.com.dius.pact.model

import org.apache.commons.collections4.iterators.PushbackIterator

sealed class PathToken {
  object Root : PathToken()
  data class Field(val name: String) : PathToken()
  data class Index(val index: Int) : PathToken()
  object Star : PathToken()
  object StarIndex : PathToken()
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

// identifier -> a-zA-Z0-9\-+
fun identifier(ch: Char, chars: PushbackIterator<IndexedValue<Char>>, tokens: MutableList<PathToken>, path: String) {
  var id = String() + ch
  while (chars.hasNext()) {
    val c = chars.next()
    if (c.value.isLetterOrDigit() || c.value == '-' || c.value == '_') {
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
fun pathIdentifier(chars: PushbackIterator<IndexedValue<Char>>, tokens: MutableList<PathToken>, path: String, index: Int) {
  if (chars.hasNext()) {
    val ch = chars.next()
    when {
      ch.value == '*' -> tokens.add(PathToken.Star)
      ch.value.isLetterOrDigit() || EXP_ALLOWED_SPECIAL_CHARS.contains(ch.value) -> identifier(ch.value, chars, tokens, path)
      else -> throw InvalidPathExpression("Expected either a \"*\" or path identifier in path expression \"$path\"" +
        " at index ${ch.index}")
    }
  } else {
    throw InvalidPathExpression("Expected a path after \".\" in path expression \"$path\" at index $index")
  }
}

// bracket_path -> (string_path | index | *) ]
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
