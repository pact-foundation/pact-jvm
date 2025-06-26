package au.com.dius.pact.core.model

import au.com.dius.pact.core.model.Into
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

//  /// Construct a new DocPath from a list of tokens
//  pub fn from_tokens<I>(tokens: I) -> Self
//    where I: IntoIterator<Item = PathToken> {
//    let mut path = Self {
//      path_tokens: tokens.into_iter().collect(),
//      expr: "".into(),
//    };
//    path.expr = path.build_expr();
//    path
//  }
//
//  /// Mutates this path by pushing a path token onto the end.
//  pub fn push(&mut self, path_token: PathToken) -> &mut Self {
//    match &path_token {
//      PathToken::Root => self.expr.push_str("$"),
//      PathToken::Field(v) => {
//        let s = &mut self.expr;
//        write_obj_key_for_path(s, v.as_str())
//      },
//      PathToken::Index(i) => { let _ = write!(self.expr, "[{}]", i); },
//      PathToken::Star => self.expr.push_str(".*"),
//      PathToken::StarIndex => self.expr.push_str("[*]")
//    };
//    self.path_tokens.push(path_token);
//    self
//  }
//
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
//
//  /// Return the parent path from this one
//  pub fn parent(&self) -> Option<Self> {
//    if self.path_tokens.len() <= 1 {
//      None
//    } else {
//      let mut vec = self.path_tokens.clone();
//      vec.truncate(vec.len() - 1);
//      let mut path = DocPath {
//        path_tokens: vec,
//        expr: "".to_string()
//      };
//      path.expr = path.build_expr();
//      Some(path)
//    }
//  }
//
//  fn build_expr(&self) -> String {
//    let mut buffer = String::new();
//
//    for token in &self.path_tokens {
//      match token {
//        PathToken::Root => buffer.push('$'),
//        PathToken::Field(v) => {
//          write_obj_key_for_path(&mut buffer, v.as_str());
//        }
//        PathToken::Index(i) => {
//          let _ = write!(buffer, "[{}]", i);
//        }
//        PathToken::Star => {
//          buffer.push('.');
//          buffer.push('*');
//        }
//        PathToken::StarIndex => {
//          buffer.push('[');
//          buffer.push('*');
//          buffer.push(']');
//        }
//      }
//    }
//
//    buffer
//  }
//
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
//  /// Converts this path into a JSON pointer [RFC6901](https://datatracker.ietf.org/doc/html/rfc6901).
//  pub fn as_json_pointer(&self) -> anyhow::Result<String> {
//    let mut buffer = String::new();
//
//    for token in &self.path_tokens {
//      match token {
//        PathToken::Root => {},
//        PathToken::Field(v) => {
//          let parsed = v.replace('~', "~0")
//            .replace('/', "~1");
//          let _ = write!(buffer, "/{}", parsed);
//        }
//        PathToken::Index(i) => {
//          buffer.push('/');
//          buffer.push_str(i.to_string().as_str());
//        }
//        PathToken::Star => {
//          return Err(anyhow!("* can not be converted to a JSON pointer"));
//        }
//        PathToken::StarIndex => {
//          return Err(anyhow!("* can not be converted to a JSON pointer"));
//        }
//      }
//    }
//
//    Ok(buffer)
//  }
//
//  /// If this path (as a string) ends with the given string
//  pub fn ends_with(&self, suffix: &str) -> bool {
//    self.expr.ends_with(suffix)
//  }
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
//
//impl From<DocPath> for String {
//  fn from(doc_path: DocPath) -> String {
//    doc_path.expr
//  }
//}
//
//impl From<&DocPath> for String {
//  fn from(doc_path: &DocPath) -> String {
//    doc_path.expr.clone()
//  }
//}
//
//impl TryFrom<String> for DocPath {
//  type Error = anyhow::Error;
//
//  fn try_from(path: String) -> Result<Self, Self::Error> {
//    DocPath::new(path)
//  }
//}
//
//impl TryFrom<&String> for DocPath {
//  type Error = anyhow::Error;
//
//  fn try_from(path: &String) -> Result<Self, Self::Error> {
//    DocPath::new(path)
//  }
//}
//
//impl TryFrom<&str> for DocPath {
//  type Error = anyhow::Error;
//
//  fn try_from(path: &str) -> Result<Self, Self::Error> {
//    DocPath::new(path)
//  }
//}
//
//impl From<&DocPath>  for DocPath {
//  fn from(value: &DocPath) -> Self {
//    value.clone()
//  }
//}

//impl PartialOrd for DocPath {
//  fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
//    Some(self.cmp(other))
//  }
//}
//
//impl Ord for DocPath {
//  fn cmp(&self, other: &Self) -> Ordering {
//    self.expr.cmp(&other.expr)
//  }
//}

@Suppress("TooManyFunctions")
data class DocPath(
  val pathTokens: List<PathToken>,
  val expr: String
): Into<DocPath> {
  /** Construct a new document path from the provided string path */
  constructor(expression: String): this(parsePath(expression), expression)

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
  fun <S> join(part: S): DocPath where S: Into<String> {
    return when (val part = part.into()) {
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
   * Pushes a field value onto the end of the path
   */
  fun <S> pushField(field: S): DocPath where S: Into<String> {
    val field = field.into()
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
//

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
  }
}
