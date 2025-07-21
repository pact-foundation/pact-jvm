package au.com.dius.pact.core.model

import au.com.dius.pact.core.support.Result
import spock.lang.Specification

@SuppressWarnings('LineLength')
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
    !new DocPath('').wildcard
    !new DocPath('$.path').wildcard
    new DocPath('$.*').wildcard
    new DocPath('$.pat.*').wildcard
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

  def 'writeObjKeyForPath quotes keys when necessary'() {
    expect:
    DocPath.writeObjKeyForPath('', path) == result

    where:

    path   | result
    'foo'  | '.foo'
    '_foo' | '._foo'
    '['    | "['[']"
    "''"   | "['\\'\\'']"
    "a'"   | "['a\\'']"
    '\\'   | "['\\\\']"
  }

  def join() {
    expect:
    path.join(part).expr == result

    where:

    path                       | part             | result
    DocPath.root()             | 'something'      | '$.something'
    DocPath.root()             | 'something else' | "\$['something else']"
    new DocPath('$.something') | 'else'           | '$.something.else'
    new DocPath('$.something') | '101'            | '$.something[101]'
  }

  def 'path push'() {
    expect:
    path.push(part).toString() == result

    where:

    path                       | part                                  | result
    DocPath.root()             | new PathToken.Field('something')      | '$.something'
    DocPath.root()             | new PathToken.Field('something else') | "\$['something else']"
    new DocPath('$.something') | new PathToken.Field('else')           | '$.something.else'
    new DocPath('$.something') | PathToken.Star.INSTANCE               | '$.something.*'
    new DocPath('$.something') | PathToken.StarIndex.INSTANCE          | '$.something[*]'
    new DocPath('$.something') | new PathToken.Index(101)              | '$.something[101]'
  }

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

  def 'buildExpr'() {
    expect:
    DocPath.buildExpr(tokens) == result

    where:

    tokens                                                                                                         | result
    [PathToken.Root.INSTANCE]                                                                                      | '$'
    [PathToken.Root.INSTANCE, new PathToken.Field('something')]                                                    | '$.something'
    [PathToken.Root.INSTANCE, new PathToken.Field('something else')]                                               | "\$['something else']"
    [PathToken.Root.INSTANCE, new PathToken.Field('something'), new PathToken.Field('else')]                       | '$.something.else'
    [PathToken.Root.INSTANCE, new PathToken.Field('something'), PathToken.Star.INSTANCE]                           | '$.something.*'
    [PathToken.Root.INSTANCE, new PathToken.Field('something'), PathToken.StarIndex.INSTANCE]                      | '$.something[*]'
    [PathToken.Root.INSTANCE, new PathToken.Field('something'), new PathToken.Index(101)]                          | '$.something[101]'
    [PathToken.Root.INSTANCE, new PathToken.Field('something'), PathToken.Star.INSTANCE, new PathToken.Index(101)] | '$.something.*[101]'
  }

  def path_parent() {
    expect:
    path.parent() == parent

    where:

    path                                                         | parent
    DocPath.empty()                                              | null
    DocPath.root()                                               | null
    new DocPath([new PathToken.Field('test')])                   | null
    DocPath.root().join('something')                             | DocPath.root()
    DocPath.root().join('something').join('else')                | DocPath.root().join('something')
    DocPath.root().join('something').join('*')                   | DocPath.root().join('something')
    DocPath.root().join('something').join('e l s e')             | DocPath.root().join('something')
    DocPath.root().join('something').join('e l s e').join('two') | DocPath.root().join('something').join('e l s e')
    DocPath.root().join('something').join('*').join('child')     | DocPath.root().join('something').join('*')
  }

  def 'as json pointer'() {
    expect:
    path.asJsonPointer() == result

    where:

    path                                            | result
    DocPath.root()                                  | new Result.Ok('')
    DocPath.root().join('something')                | new Result.Ok('/something')
    DocPath.root().join('something else')           | new Result.Ok('/something else')
    DocPath.root().join('something').join('a/b')    | new Result.Ok('/something/a~1b')
    DocPath.root().join('something').join('m~n')    | new Result.Ok('/something/m~0n')
    DocPath.root().join('something').join('c%25d')  | new Result.Ok('/something/c%25d')
    DocPath.root().join('something').join('*')      | new Result.Err('* can not be converted to a JSON pointer')
    DocPath.root().join('something').pushIndex(101) | new Result.Ok('/something/101')
  }

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
