package au.com.dius.pact.core.matchers

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings(['LineLength', 'UnnecessaryBooleanExpression'])
class RequestMatchResultSpec extends Specification {
  @Shared
  private static MethodMismatch methodMismatch
  @Shared
  private static PathMismatch pathMismatch
  @Shared
  private static QueryMatchResult queryMismatch
  @Shared
  private static CookieMismatch cookieMismatch
  @Shared
  private static HeaderMatchResult headerMismatch
  @Shared
  private static BodyItemMatchResult bodyMismatch

  def setupSpec() {
    methodMismatch = new MethodMismatch('GET', 'POST')
    pathMismatch = new PathMismatch('/', '/1')
    queryMismatch = new QueryMatchResult('a', [new QueryMismatch('a', '1', '2', '', '')])
    cookieMismatch = new CookieMismatch([], [])
    headerMismatch = new HeaderMatchResult('a', [new HeaderMismatch('a', '1', '2', '')])
    bodyMismatch = new BodyItemMatchResult('$.a', [new BodyMismatch('a', 'b', '')])
  }

  @Unroll
  def 'test for match result scoring'() {
    expect:
    new RequestMatchResult(method, path, query, cookie, headers, new BodyMatchResult(null, body)).calculateScore() == score

    where:

    method         | path         | query           | cookie         | headers          | body           || score
    null           | null         | []              | null           | []               | []             || 3
    methodMismatch | null         | []              | null           | []               | []             || 1
    null           | pathMismatch | []              | null           | []               | []             || 1
    null           | null         | [queryMismatch] | null           | []               | []             || 2
    null           | null         | []              | cookieMismatch | []               | []             || 1
    null           | null         | []              | null           | [headerMismatch] | []             || 2
    null           | null         | []              | null           | []               | [bodyMismatch] || 2
    methodMismatch | null         | []              | null           | []               | []             || 1
    methodMismatch | pathMismatch | []              | null           | []               | []             || -1
    methodMismatch | null         | [queryMismatch] | null           | []               | []             || 0
    methodMismatch | null         | []              | cookieMismatch | []               | []             || -1
    methodMismatch | null         | []              | null           | [headerMismatch] | []             || 0
    methodMismatch | null         | []              | null           | []               | [bodyMismatch] || 0
  }

  @Unroll
  def 'query matching scoring'() {
    expect:
    new RequestMatchResult(null, null, query, null, [], new BodyMatchResult(null, [])).calculateScore() == score

    where:

    query                                                                                                                                          || score
    []                                                                                                                                             || 3
    [new QueryMatchResult('a', [new QueryMismatch('a', '1', '2', '', '')])]                                                                        || 2
    [new QueryMatchResult('a', [])]                                                                                                                || 4
    [new QueryMatchResult('a', [new QueryMismatch('a', '1', '2', '', '')]), new QueryMatchResult('b', [new QueryMismatch('b', '1', '2', '', '')])] || 1
    [new QueryMatchResult('a', [new QueryMismatch('a', '1', '2', '', '')]), new QueryMatchResult('b', [])]                                         || 3
    [new QueryMatchResult('a', []), new QueryMatchResult('b', [])]                                                                                 || 5
  }

  @Unroll
  def 'header matching scoring'() {
    expect:
    new RequestMatchResult(null, null, [], null, header, new BodyMatchResult(null, [])).calculateScore() == score

    where:

    header                                                                                                                                     || score
    []                                                                                                                                         || 3
    [new HeaderMatchResult('a', [new HeaderMismatch('a', '1', '2', '')])]                                                                      || 2
    [new HeaderMatchResult('a', [])]                                                                                                           || 4
    [new HeaderMatchResult('a', [new HeaderMismatch('a', '1', '2', '')]), new HeaderMatchResult('b', [new HeaderMismatch('b', '1', '2', '')])] || 1
    [new HeaderMatchResult('a', [new HeaderMismatch('a', '1', '2', '')]), new HeaderMatchResult('b', [])]                                      || 3
    [new HeaderMatchResult('a', []), new HeaderMatchResult('b', [])]                                                                           || 5
  }

  @Unroll
  def 'body matching scoring'() {
    expect:
    new RequestMatchResult(null, null, [], null, [], body).calculateScore() == score

    where:

    body                                                                                                                                                        || score
    new BodyMatchResult(null, [])                                                                                                                               || 3
    new BodyMatchResult(new BodyTypeMismatch('', ''), [])                                                                                                       || 2
    new BodyMatchResult(new BodyTypeMismatch('', ''), [new BodyItemMatchResult('a', [])])                                                                       || 2
    new BodyMatchResult(new BodyTypeMismatch('', ''), [new BodyItemMatchResult('a', [new BodyMismatch('a', 'b', '')])])                                         || 2
    new BodyMatchResult(new BodyTypeMismatch('', ''), [new BodyItemMatchResult('a', []), new BodyItemMatchResult('b', [])])                                     || 2
    new BodyMatchResult(null, [new BodyItemMatchResult('a', [new BodyMismatch('a', 'b', '')])])                                                                 || 2
    new BodyMatchResult(null, [new BodyItemMatchResult('a', [])])                                                                                               || 4
    new BodyMatchResult(null, [new BodyItemMatchResult('a', [new BodyMismatch('a', 'b', '')]), new BodyItemMatchResult('b', [new BodyMismatch('a', 'b', '')])]) || 1
    new BodyMatchResult(null, [new BodyItemMatchResult('a', []), new BodyItemMatchResult('b', [new BodyMismatch('a', 'b', '')])])                               || 3
    new BodyMatchResult(null, [new BodyItemMatchResult('a', []), new BodyItemMatchResult('b', [])])                                                             || 5

  }
}
