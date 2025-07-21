package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Result
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

//  /// Mutates this path by pushing another path onto the end. Will drop the root marker from the
//  /// other path
//  pub fn push_path(&mut self, path: &DocPath) -> &mut Self {
//    for token in &path.path_tokens {
//      if token != &PathToken::Root {
//        self.push(token.clone());
//      }
//    }
//    self
//  }

//  /// Returns a copy of this path will all parts lower case
//  pub fn to_lower_case(&self) -> DocPath {
//    DocPath {
//      path_tokens: self.path_tokens.iter().map(|p| match p {
//        PathToken::Field(f) => PathToken::Field(f.to_lowercase()),
//        _ => p.clone()
//      }).collect(),
//      expr: self.expr.to_lowercase()
//    }
//  }
//
//
//  /// Creates a new path with the last `n` parts removed.
//  pub fn drop(&self, n: usize) -> Self {
//    let vec = self.path_tokens.iter()
//      .dropping_back(n)
//      .cloned()
//      .collect_vec();
//    if vec.is_empty() {
//      Self::root()
//    } else {
//      let mut path = DocPath {
//        path_tokens: vec,
//        expr: "".to_string()
//      };
//      path.expr = path.build_expr();
//      path
//    }
//  }
//}

@Suppress("TooManyFunctions")
data class DocPath(
  val pathTokens: List<PathToken>,
  val expr: String
): Into<DocPath> {
  /** Construct a new document path from the provided string path */
  constructor(expression: String): this(parsePath(expression), expression)

  /** Construct a new DocPath from a list of tokens */
  constructor(tokens: List<PathToken>): this(tokens, buildExpr(tokens))

  override fun into() = this.copy()

  override fun toString() = this.expr

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DocPath

    return expr == other.expr
  }

  override fun hashCode(): Int {
    return expr.hashCode()
  }

  fun asList() = pathTokens.map { it.rawString() }

  /** If this path is the root path (it has only one element, the root token `$`). */
  fun isRoot() = pathTokens == listOf(PathToken.Root)

  /** The path is a wildcard path if it ends in a star (`*`) */
  fun isWildcard() = pathTokens.lastOrNull() == PathToken.Star

  /** Return the length, in parsed tokens. */
  fun len() = pathTokens.size

  /**
   * Calculates the path weight for this path expression and a given path.
   * Returns a tuple of the calculated weight and the number of path tokens matched.
   */
  fun pathWeight(path: List<String>): Pair<Int, Int> {
    logger.trace { "Calculating weight for path tokens '$pathTokens' and path '$path'" }
    val weight = if (path.size >= len()) {
      calculatePathWeight(pathTokens, path) to len()
    } else {
      0 to len()
    }
    logger.trace { "Calculated weight $weight for path '$path' and '$expr'" }
    return weight
  }

  /**
   * If this path matches the given path. It will match if the calculated path weight is greater
   * than zero (which means at least one token matched).
   */
  fun matchesPath(path: List<String>) = pathWeight(path).first > 0

  /**
   * If the path matches the given path (the calculated path weight is greater than zero) and
   * both paths have the same length.
   */
  fun matchesPathExactly(path: List<String>) = len() == path.size && matchesPath(path)

  /** Return the list of tokens that comprise this path. */
  fun tokens() = pathTokens

  /** Returns the last item in the path, or null if empty. */
  fun last() = pathTokens.lastOrNull()

  /** Extract the string contents of the first Field token. For use with Header and Query DocPaths. */
  fun firstField(): String? {
    val result = pathTokens.firstOrNull { it is PathToken.Field }
    return if (result is PathToken.Field) {
      result.name
    } else {
      null
    }
  }

  /** Extract the string contents of the last Field token. */
  fun lastField(): String? {
    val result = pathTokens.lastOrNull { it is PathToken.Field }
    return if (result is PathToken.Field) {
      result.name
    } else {
      null
    }
  }

  /**
   * Creates a new path by cloning this one and pushing the string onto the end
   */
  fun join(part: String): DocPath {
    return when (part) {
      "*" -> pushStar()
      "[*]" -> pushStarIndex()
      else -> {
        try {
          val index = part.toInt()
          pushIndex(index)
        } catch (_ex: NumberFormatException) {
          pushField(Into { part })
        }
      }
    }
  }

  /**
   * Creates a new path by cloning this one and pushing the string onto the end
   */
  fun <S> join(part: S): DocPath where S: Into<String> {
    return join(part.into())
  }

  /**
   * Pushes a field value onto the end of the path
   */
  fun <S> pushField(field: S): DocPath where S: Into<String> {
    return pushField(field.into())
  }

  /**
   * Pushes a field value onto the end of the path
   */
  fun pushField(field: String): DocPath {
    val expr = writeObjKeyForPath(expr, field)
    val pathTokens = pathTokens + PathToken.Field(field)
    return DocPath(pathTokens, expr)
  }

  /**
   * Pushes an index value onto the end of the path
   */
  fun pushIndex(index: Int): DocPath {
    val pathTokens = pathTokens + PathToken.Index(index)
    val expr = "$expr[$index]"
    return DocPath(pathTokens, expr)
  }

  /**
   * Pushes a star value onto the end of the path
   */
  fun pushStar(): DocPath {
    val pathTokens = pathTokens + PathToken.Star
    val expr = "$expr.*"
    return DocPath(pathTokens, expr)
  }

  /**
   * Pushes a star index value onto the end of the path
   */
  fun pushStarIndex(): DocPath {
    val pathTokens = pathTokens + PathToken.Star
    val expr = "$expr[*]"
    return DocPath(pathTokens, expr)
  }

  /** Pushes a path token onto the end of the path */
  fun push(pathToken: PathToken): DocPath {
    val exp = when (pathToken) {
      is PathToken.Field -> writeObjKeyForPath(expr, pathToken.name)
      is PathToken.Index -> expr + "[${pathToken.index}]"
      PathToken.Root -> "$expr$"
      PathToken.Star -> "$expr.*"
      PathToken.StarIndex -> "$expr[*]"
    }
    return DocPath(pathTokens + pathToken, exp)
  }

//  /// Creates a new path by cloning this one and joining the index onto the end. Paths that end
//  /// with `*` will have the `*` replaced with the index.
//  pub fn join_index(&self, index: usize) -> Self {
//    let mut path = self.clone();
//    match self.path_tokens.last() {
//      Some(PathToken::Root) => { path.push_index(index); }
//      Some(PathToken::Field(_)) => { path.push_index(index); }
//      Some(PathToken::Index(_)) => { path.push_index(index); }
//      Some(PathToken::Star) | Some(PathToken::StarIndex) => {
//        if let Some(part) = path.path_tokens.last_mut() {
//          *part = PathToken::Index(index);
//          path.expr = path.build_expr();
//        } else {
//          path.push_index(index);
//        }
//      }
//      None => { path.push_index(index); }
//    }
//    path
//  }
//
//  /// Creates a new path by cloning this one and joining the field onto the end. Paths that end
//  /// with `*` will have the `*` replaced with the field.
//  pub fn join_field<S: Into<String>>(&self, name: S) -> Self {
//    let mut path = self.clone();
//    match self.path_tokens.last() {
//      Some(PathToken::Root) => { path.push_field(name.into()); }
//      Some(PathToken::Field(_)) => { path.push_field(name.into()); }
//      Some(PathToken::Index(_)) => { path.push_field(name.into()); }
//      Some(PathToken::Star) => {
//        if let Some(part) = path.path_tokens.last_mut() {
//          *part = PathToken::Field(name.into());
//          path.expr = path.build_expr();
//        } else {
//          path.push_field(name.into());
//        }
//      }
//      Some(PathToken::StarIndex) => { path.push_field(name.into()); }
//      None => { path.push_field(name.into()); }
//    }
//    path
//  }

  /** If this path (as a string) ends with the given string */
  fun endsWith(suffix: String): Boolean = this.expr.endsWith(suffix)

  /** Converts this path into a JSON pointer [RFC6901](https://datatracker.ietf.org/doc/html/rfc6901). */
  fun asJsonPointer(): Result<String, String> {
    val buffer = StringBuilder()

    for (token in pathTokens) {
      when (token) {
        is PathToken.Field -> {
          val parsed = token.name.replace("~", "~0")
            .replace("/", "~1")
          buffer.append('/').append(parsed)
        }
        is PathToken.Index -> {
          buffer.append('/').append(token.index.toString())
        }
        PathToken.Root -> {}
        PathToken.Star -> {
          return Result.Err("* can not be converted to a JSON pointer")
        }
        PathToken.StarIndex -> {
          return Result.Err("[*] can not be converted to a JSON pointer")
        }
      }
    }

    return Result.Ok(buffer.toString())
  }

  /** Return the parent path from this one */
  fun parent(): DocPath? {
    return if (pathTokens.size <= 1) {
      null
    } else {
      DocPath(pathTokens.dropLast(1))
    }
  }

  companion object {
    val IDENT = Regex("^[_A-Za-z][_A-Za-z0-9]*$")

    /**
     *  Construct a new DocPath with an empty expression.
     *
     * Warning: do not call any of the `push*` methods on this DocPath,
     * as that would create an expression with invalid syntax
     * (because it would be missing the Root token).
     */
    @JvmStatic
    fun empty() = DocPath(emptyList(), "")

    /**
     *  Construct a new DocPath with the Root token.
     */
    @JvmStatic
    fun root() = DocPath(listOf(PathToken.Root), "$")

    fun calculatePathWeight(pathTokens: List<PathToken>, path: List<String>): Int {
      return path
        .zip(pathTokens) { pathElement, pathToken -> matchesToken(pathElement, pathToken) }
        .reduce { acc, i -> acc * i }
    }

    fun matchesToken(pathElement: String, token: PathToken): Int {
      return when (token) {
        is PathToken.Root -> if (pathElement == "$") 2 else 0
        is PathToken.Field -> if (pathElement == token.name) 2 else 0
        is PathToken.Index -> if (pathElement.toIntOrNull() == token.index) 2 else 0
        is PathToken.StarIndex -> if (pathElement.toIntOrNull() != null) 1 else 0
        is PathToken.Star -> 1
        else -> 0
      }
    }

    /**
     * Format a JSON object key for use in a JSON path expression. If we were
     * more concerned about performance, we might try to come up with a scheme
     * to minimize string allocation here.
     */
    @JvmStatic
    fun writeObjKeyForPath(expr: String, key: String): String {
      return expr + if (IDENT.matches(key)) {
        ".${key}"
      } else {
        "['${escape(key)}']"
      }
    }

    private fun escape(key: String): String {
      val buffer = StringBuilder()
      for (ch in key) {
        when (ch) {
          '\\', '\'', '|' -> {
            buffer.append('\\')
            buffer.append(ch)
          }
          else -> buffer.append(ch)
        }
      }
      return buffer.toString()
    }

    @JvmStatic
    fun buildExpr(tokens: List<PathToken>): String {
      return tokens.joinToString("") {
        when (it) {
          is PathToken.Field -> {
            if (IDENT.matches(it.name)) {
              ".${it.name}"
            } else {
              "['${escape(it.name)}']"
            }
          }
          is PathToken.Index -> "[${it.index}]"
          PathToken.Root -> "$"
          PathToken.Star -> ".*"
          PathToken.StarIndex -> "[*]"
        }
      }
    }
  }
}
