package au.com.dius.pact.consumer.groovy

import org.junit.Before
import org.junit.Test

class MatchersTest {

  private Matchers matchers

  @Before
  void setup() {
    matchers = new Matchers()
  }

  @Test(expected = InvalidMatcherException)
  void 'regexp matcher fails if the regular expression does not match the example'() {
    matchers.regexp('[a-z]+', 'aksdfkdshfkjdhf23876r872')
  }

  @Test
  void 'regexp matcher does not fail if the regular expression matches the example'() {
    matchers.regexp('[a-z0-9]+', 'aksdfkdshfkjdhf23876r872')
  }

  @Test(expected = InvalidMatcherException)
  void 'regexp matcher with a pattern fails if the regular expression does not match the example'() {
    matchers.regexp(~/[a-z]+/, 'aksdfkdshfkjdhf23876r872')
  }

  @Test
  void 'regexp matcher with a pattern does not fail if the regular expression matches the example'() {
    matchers.regexp(~/[a-z0-9]+/, 'aksdfkdshfkjdhf23876r872')
  }

  @Test(expected = InvalidMatcherException)
  void 'hexadecimal matcher fails if the the example is not a hexadecimal number'() {
    matchers.hexValue('aksdfkdshfkjdhf23876r872')
  }

  @Test
  void 'hexadecimal matcher does not fail if the the example is a hexadecimal number'() {
    matchers.hexValue('afdfdf23876872')
  }

  @Test(expected = InvalidMatcherException)
  void 'ip address matcher fails if the the example is not an ip address'() {
    matchers.ipAddress('aksdfkdshfkjdhf23876r872')
  }

  @Test
  void 'ip address matcher does not fail if the the example is an ipaddress'() {
    matchers.ipAddress('10.10.100.1')
  }

  @Test(expected = InvalidMatcherException)
  void 'timestamp matcher fails if the the example does not match the given pattern'() {
    matchers.timestamp('yyyyMMddhh', '2001101014')
  }

  @Test
  void 'timestamp matcher does not fail if the the example matches the pattern'() {
    matchers.timestamp('yyyyMMddhh', '2001101002')
  }

  @Test(expected = InvalidMatcherException)
  void 'date matcher fails if the the example does not match the given pattern'() {
    matchers.date('yyyyMMdd', '20011410')
  }

  @Test
  void 'date matcher does not fail if the the example matches the pattern'() {
    matchers.date('yyyyMMdd', '20011010')
  }

  @Test(expected = InvalidMatcherException)
  void 'time matcher fails if the the example does not match the given pattern'() {
    matchers.date('HHmmss', '147812')
  }

  @Test
  void 'time matcher does not fail if the the example matches the pattern'() {
    matchers.date('HH:mm:ss.SSS', '14:34:32.678')
  }

  @Test(expected = InvalidMatcherException)
  void 'guid matcher fails if the the example is not a guid'() {
    matchers.guid('aksdfkdshfkjdhf23876r872')
  }

  @Test
  void 'guid matcher does not fail if the the example is a guid'() {
    matchers.guid('74a7c275-ee8b-4019-b4eb-3e37f7cde95f')
  }

}
