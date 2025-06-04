package au.com.dius.pact.core.model

import spock.lang.Specification

class DocPathSpec extends Specification {
  def 'empty'() {
    expect:
    DocPath.empty().pathTokens == new DocPath('').pathTokens
  }

  def 'root'() {
    expect:
    DocPath.root().pathTokens == new DocPath('$').pathTokens
  }

  def 'wildcard'() {
    expect:
    !new DocPath('').isWildcard()
    !new DocPath('$.path').isWildcard()
    new DocPath('$.*').isWildcard()
    new DocPath('$.pat.*').isWildcard()
  }

  def 'matches path - matches root path element'() {
    expect:
    DocPath.root().matchesPath(['$'])
    !DocPath.root().matchesPath([])
  }

  def 'matches path - matches field name'() {
    expect:
    new DocPath('$.name').matchesPath(['$', 'name'])
    new DocPath("\$['name']").matchesPath(['$', 'name'])
    new DocPath('$.name.other').matchesPath(['$', 'name', 'other'])
    new DocPath("\$['name'].other").matchesPath(['$', 'name', 'other'])
    !new DocPath('$.name').matchesPath(['$', 'other'])
    new DocPath('$.name').matchesPath(['$', 'name', 'other'])
    !new DocPath('$.other').matchesPath(['$', 'name', 'other'])
    !new DocPath('$.name.other').matchesPath(['$', 'name'])
  }

  def 'matches path - matches array indices'() {
    expect:
    new DocPath('$[0]').matchesPath(['$', '0'])
    new DocPath('$.name[1]').matchesPath(['$', 'name', '1'])
    !new DocPath('$.name').matchesPath(['$', '0'])
    !new DocPath('$.name[1]').matchesPath(['$', 'name', '0'])
    !new DocPath('$[1].name').matchesPath(['$', 'name', '1'])
  }

  def 'matches path - matches with wildcard'() {
    expect:
    new DocPath('$[*]').matchesPath(['$', '0'])
    new DocPath('$.*').matchesPath(['$', 'name'])
    new DocPath('$.*.name').matchesPath(['$', 'some', 'name'])
    new DocPath('$.name[*]').matchesPath(['$', 'name', '0'])
    new DocPath('$.name[*].name').matchesPath(['$', 'name', '1', 'name'])
    !new DocPath('$[*]').matchesPath(['$', 'name'])
  }

  //  #[test]
  //  fn obj_key_for_path_quotes_keys_when_necessary() {
  //    assert_eq!(obj_key_for_path("foo"), ".foo");
  //    assert_eq!(obj_key_for_path("_foo"), "._foo");
  //    assert_eq!(obj_key_for_path("["), "['[']");
  //
  //    // I don't actually know how the JSON Path specification wants us to handle
  //    // these cases, but we need to _something_ to avoid panics or passing
  //    // `Result` around everywhere, so let's go with JavaScript string escape
  //    // syntax.
  //    assert_eq!(obj_key_for_path(r#"''"#), r#"['\'\'']"#);
  //    assert_eq!(obj_key_for_path(r#"a'"#), r#"['a\'']"#);
  //    assert_eq!(obj_key_for_path(r#"\"#), r#"['\\']"#);
  //  }
  //
  //  #[test]
  //  fn path_join() {
  //    let something = DocPath::root().join("something");
  //    expect!(something.to_string()).to(be_equal_to("$.something"));
  //    expect!(DocPath::root().join("something else").to_string()).to(be_equal_to("$['something else']"));
  //    expect!(something.join("else").to_string()).to(be_equal_to("$.something.else"));
  //    expect!(something.join("*").to_string()).to(be_equal_to("$.something.*"));
  //    expect!(something.join("101").to_string()).to(be_equal_to("$.something[101]"));
  //  }
  //
  //  #[test]
  //  fn path_push() {
  //    let mut root = DocPath::root();
  //    let something = root.push(PathToken::Field("something".to_string()));
  //    expect!(something.to_string()).to(be_equal_to("$.something"));
  //    expect!(DocPath::root().push(PathToken::Field("something else".to_string())).to_string())
  //      .to(be_equal_to("$['something else']"));
  //    expect!(something.push(PathToken::Field("else".to_string())).to_string())
  //      .to(be_equal_to("$.something.else"));
  //    expect!(something.push(PathToken::Star).to_string())
  //      .to(be_equal_to("$.something.else.*"));
  //    expect!(something.push(PathToken::Index(101)).to_string())
  //      .to(be_equal_to("$.something.else.*[101]"));
  //  }
  //
  //  #[test]
  //  fn push_path() {
  //    let mut empty = DocPath::empty();
  //    let a = empty.push_field("a");
  //    let mut root = DocPath::root();
  //    let b = root.push_field("a").push_field("b");
  //    let mut root = DocPath::root();
  //    let c = root.push_field("a").push_field("b").push_field("se-token");
  //    expect!(DocPath::root().push_path(a).to_string())
  //      .to(be_equal_to("$.a"));
  //    expect!(DocPath::root().push_path(b).to_string())
  //      .to(be_equal_to("$.a.b"));
  //    expect!(DocPath::root().push_path(c).to_string())
  //      .to(be_equal_to("$.a.b['se-token']"));
  //  }
  //
  //  #[test]
  //  fn build_expr() {
  //    let mut root = DocPath::root();
  //    expect!(root.build_expr()).to(be_equal_to("$"));
  //    let something = root.push(PathToken::Field("something".to_string()));
  //    expect!(something.build_expr()).to(be_equal_to("$.something"));
  //    expect!(DocPath::root().push(PathToken::Field("something else".to_string())).build_expr())
  //      .to(be_equal_to("$['something else']"));
  //    expect!(something.push(PathToken::Field("else".to_string())).build_expr())
  //      .to(be_equal_to("$.something.else"));
  //    expect!(something.push(PathToken::Star).build_expr())
  //      .to(be_equal_to("$.something.else.*"));
  //    expect!(something.push(PathToken::Index(101)).build_expr())
  //      .to(be_equal_to("$.something.else.*[101]"));
  //  }
  //
  //  #[test]
  //  fn path_parent() {
  //    let something = DocPath::root().join("something");
  //    let something_else = something.join("else");
  //    let something_star = something.join("*");
  //    let something_escaped = something.join("e l s e");
  //    let something_escaped2 = something_escaped.join("two");
  //    let something_star_child = something_star.join("child");
  //
  //    expect!(something.parent()).to(be_some().value(DocPath::root()));
  //    expect!(something_else.parent()).to(be_some().value(something.clone()));
  //    expect!(something_star.parent()).to(be_some().value(something.clone()));
  //    expect!(something_escaped.parent()).to(be_some().value(something.clone()));
  //    expect!(something_escaped2.parent()).to(be_some().value(something_escaped.clone()));
  //    expect!(something_star_child.parent()).to(be_some().value(something_star.clone()));
  //
  //    expect!(DocPath::root().parent()).to(be_none());
  //    expect!(DocPath::empty().parent()).to(be_none());
  //  }
  //
  //  #[test]
  //  fn as_json_pointer() {
  //    let root = DocPath::root();
  //    expect!(root.as_json_pointer().unwrap()).to(be_equal_to(""));
  //
  //    let mut something = root.join("something");
  //    expect!(something.as_json_pointer().unwrap()).to(be_equal_to("/something"));
  //
  //    let with_slash = something.join("a/b");
  //    expect!(with_slash.as_json_pointer().unwrap()).to(be_equal_to("/something/a~1b"));
  //    let with_tilde = something.join("m~n");
  //    expect!(with_tilde.as_json_pointer().unwrap()).to(be_equal_to("/something/m~0n"));
  //
  //    let encoded = something.join("c%25d");
  //    expect!(encoded.as_json_pointer().unwrap()).to(be_equal_to("/something/c%25d"));
  //
  //    expect!(DocPath::root().push(PathToken::Field("something else".to_string())).as_json_pointer().unwrap())
  //      .to(be_equal_to("/something else"));
  //    expect!(something.join("*").as_json_pointer()).to(be_err());
  //    expect!(something.push(PathToken::Index(101)).as_json_pointer().unwrap())
  //      .to(be_equal_to("/something/101"));
  //  }
  //
  //  #[rstest(
  //    case("", "[0]"),
  //    case("$", "$[0]"),
  //    case("$.a", "$.a[0]"),
  //    case("$.a[1]", "$.a[1][0]"),
  //    case("$.a.*", "$.a[0]"),
  //    case("$.a[*]", "$.a[0]")
  //  )]
  //  fn join_index_test(#[case] base: &'static str, #[case] result: &str) {
  //    let base_path = DocPath::new_unwrap(base);
  //    let result_path = base_path.join_index(0);
  //    expect!(result_path.to_string()).to(be_equal_to(result));
  //  }
}
