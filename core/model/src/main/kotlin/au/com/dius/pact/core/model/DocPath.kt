package au.com.dius.pact.core.model

//  /// Infallible construction for when the expression is statically known,
//  /// intended for unit tests.
//  ///
//  /// Invalid expressions will still cause panics.
//  pub fn new_unwrap(expr: &'static str) -> Self {
//    Self::new(expr).unwrap()
//  }
//
//  /// Construct a new DocPath with an empty expression.
//  ///
//  /// Warning: do not call any of the `push_*` methods on this DocPath,
//  /// as that would create an expression with invalid syntax
//  /// (because it would be missing the Root token).
//  pub fn empty() -> Self {
//    Self {
//      path_tokens: vec![],
//      expr: "".into(),
//    }
//  }
//
//  /// Construct a new DocPath with the Root token.
//  pub fn root() -> Self {
//    Self {
//      path_tokens: vec![PathToken::Root],
//      expr: "$".into(),
//    }
//  }
//
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
//  /// Return the list of tokens that comprise this path.
//  pub fn tokens(&self) -> &Vec<PathToken> {
//    &self.path_tokens
//  }
//
//  /// Return the length, in parsed tokens.
//  pub fn len(&self) -> usize {
//    self.path_tokens.len()
//  }
//
//  /// Returns the last item in the path. Will panic if the path is empty.
//  pub fn last(&self) -> Option<PathToken> {
//    self.path_tokens.last().cloned()
//  }
//
//  /// Extract the string contents of the first Field token.
//  /// For use with Header and Query DocPaths.
//  pub fn first_field(&self) -> Option<&str> {
//    for token in self.path_tokens.iter() {
//      if let PathToken::Field(field) = token {
//        return Some(field);
//      }
//    }
//    return None;
//  }
//
//  /// Extract the string contents of the last Field token.
//  pub fn last_field(&self) -> Option<&str> {
//    for token in self.path_tokens.iter().rev() {
//      if let PathToken::Field(field) = token {
//        return Some(field);
//      }
//    }
//    return None;
//  }
//
//  /// If this path is the root path (it has only one element, the root token `$`).
//  pub fn is_root(&self) -> bool {
//    &self.path_tokens == &[PathToken::Root]
//  }
//
//  /// The path is a wildcard path if it ends in a star (`*`)
//  pub fn is_wildcard(&self) -> bool {
//    self.path_tokens.last() == Some(&PathToken::Star)
//  }
//
//  /// Calculates the path weight for this path expression and a given path.
//  /// Returns a tuple of the calculated weight and the number of path tokens matched.
//  pub fn path_weight(&self, path: &[&str]) -> (usize, usize) {
//    trace!("Calculating weight for path tokens '{:?}' and path '{:?}'",
//           self.path_tokens, path);
//    let weight = {
//      if path.len() >= self.len() {
//        (
//          self.path_tokens.iter().zip(path.iter())
//          .fold(1, |acc, (token, fragment)| acc * matches_token(fragment, token)),
//          self.len()
//        )
//      } else {
//        (0, self.len())
//      }
//    };
//    trace!("Calculated weight {:?} for path '{}' and '{:?}'",
//           weight, self, path);
//    weight
//  }
//
//  /// If this path matches the given path. It will match if the calculated path weight is greater
//  /// than zero (which means at least one token matched).
//  pub fn matches_path(&self, path: &[&str]) -> bool {
//    self.path_weight(path).0 > 0
//  }
//
//  /// If the path matches the given path (the calculated path weight is greater than zero) and
//  /// both paths have the same length.
//  pub fn matches_path_exactly(&self, path: &[&str]) -> bool {
//     self.len() == path.len() && self.matches_path(path)
//  }
//
//  /// Creates a new path by cloning this one and pushing the string onto the end
//  pub fn join(&self, part: impl Into<String>) -> Self {
//    let part = part.into();
//    let mut path = self.clone();
//    if part == "*" {
//      path.push_star();
//    } else if part == "[*]" {
//      path.push_star_index();
//    } else if let Ok(index) = part.parse() {
//      path.push_index(index);
//    } else {
//      path.push_field(part);
//    }
//    path
//  }
//
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
//  /// Mutates this path by pushing a field value onto the end.
//  pub fn push_field(&mut self, field: impl Into<String>) -> &mut Self {
//    let field = field.into();
//    write_obj_key_for_path(&mut self.expr, &field);
//    self.path_tokens.push(PathToken::Field(field));
//    self
//  }
//
//  /// Mutates this path by pushing an index value onto the end.
//  pub fn push_index(&mut self, index: usize) -> &mut Self {
//    self.path_tokens.push(PathToken::Index(index));
//    // unwrap is safe, as write! is infallible for String
//    let _ = write!(self.expr, "[{}]", index);
//    self
//  }
//
//  /// Mutates this path by pushing a star value onto the end.
//  pub fn push_star(&mut self) -> &mut Self {
//    self.path_tokens.push(PathToken::Star);
//    self.expr.push_str(".*");
//    self
//  }
//
//  /// Mutates this path by pushing a star index value onto the end.
//  pub fn push_star_index(&mut self) -> &mut Self {
//    self.path_tokens.push(PathToken::StarIndex);
//    self.expr.push_str("[*]");
//    self
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
//  /// Convert this path to a vector of strings
//  pub fn to_vec(&self) -> Vec<String> {
//    self.path_tokens.iter().map(|t| t.to_string()).collect()
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
///// Format a JSON object key for use in a JSON path expression. If we were
///// more concerned about performance, we might try to come up with a scheme
///// to minimize string allocation here.
//fn write_obj_key_for_path(mut out: impl Write, key: &str) {
//  // unwrap is safe, as write! is infallible for String
//  let _ = if IDENT.is_match(key) {
//    write!(out, ".{}", key)
//  } else {
//    write!(
//      out,
//      "['{}']",
//      ESCAPE.replace_all(key, |caps: &Captures| format!(r#"\{}"#, &caps[0]))
//    )
//  };
//}
//
//#[cfg(test)]
//fn obj_key_for_path(key: &str) -> String {
//  let mut out = String::new();
//  write_obj_key_for_path(&mut out, key);
//  out
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

data class DocPath(
  val pathTokens: List<PathToken>,
  val expr: String
): Into<DocPath> {
  /** Construct a new document path from the provided string path */
  constructor(expression: String): this(parsePath(expression), expression)

  override fun into() = this

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
}
