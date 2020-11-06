package au.com.dius.pact.core.pactbroker.util

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class HttpClientUtilsSpec extends Specification {

  @Unroll
  def 'build url - #desc'() {
    expect:
    HttpClientUtils.INSTANCE.buildUrl(url, path).toString() == expectedUrl

    where:

    desc                      | url                     | path                                     | expectedUrl
    'normal URL'              | 'http://localhost:8080' | '/path'                                  | 'http://localhost:8080/path'
    'normal URL with no path' | 'http://localhost:8080' | ''                                       | 'http://localhost:8080'
    'just a path'             | ''                      | '/path/to/get'                           | '/path/to/get'
    'Full url with the path'  | ''                      | 'http://localhost:1234/path/to/get'      | 'http://localhost:1234/path/to/get'
    'URL with spaces'         | 'http://localhost:8080' | '/path/with spaces'                      | 'http://localhost:8080/path/with%20spaces'
    'path with spaces'        | ''                      | '/path/with spaces'                      | '/path/with%20spaces'
    'Full URL with spaces'    | ''                      | 'http://localhost:1234/path/with spaces' | 'http://localhost:1234/path/with%20spaces'
    'no port'                 | 'http://localhost'      | '/path/with spaces'                      | 'http://localhost/path/with%20spaces'
    'Extra path'              | 'http://localhost/sub'  | '/extraPath/with spaces'                 | 'http://localhost/sub/extraPath/with%20spaces'
  }
}
