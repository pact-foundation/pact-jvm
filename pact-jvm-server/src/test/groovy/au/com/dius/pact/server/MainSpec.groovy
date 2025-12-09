package au.com.dius.pact.server

import groovy.json.JsonSlurper
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpResponse
import spock.lang.IgnoreIf
import spock.lang.Specification

import java.util.concurrent.TimeUnit

@IgnoreIf({ os.windows })
@IgnoreIf({ System.getenv('CI') != null })
class MainSpec extends Specification {
  def 'application command line args'() {
    when:
    def process = invokeApp('--help')
    def result = process.waitFor()
    def out = process.inputReader().text

    then:
    result == 0
    out == '''Usage: pact-jvm-server [<options>] [<port>]
    |
    |Options:
    |  -h, --host=<text>            host to bind to (defaults to localhost)
    |  -l, --mock-port-lower=<int>  lower bound to allocate mock ports (defaults to
    |                               20000)
    |  -u, --mock-port-upper=<int>  upper bound to allocate mock ports (defaults to
    |                               40000)
    |  -d, --daemon                 run as a daemon process
    |  --debug                      run with debug logging
    |  -v, --pact-version=<int>     pact version to generate for (2 or 3)
    |  -k, --keystore-path=<text>   Path to keystore
    |  -p, --keystore-password=<text>
    |                               Keystore password
    |  -s, --ssl-port=<int>         Ssl port the mock server should run on. lower
    |                               and upper bounds are ignored
    |  -b, --broker=<text>          URL of broker where to publish contracts to
    |  -t, --token=<text>           Auth token for publishing the pact to broker
    |  --help                       Show this message and exit
    |
    |Arguments:
    |  <port>  port to run on (defaults to 29999)
    |'''.stripMargin('|')
  }

  def 'start master server test'() {
    given:
    def process = invokeApp('--daemon', '31310')

    when:
    process.waitFor(500, TimeUnit.MILLISECONDS)
    def result = getRoot('31310')

    then:
    result == '{"ports": [], "paths": []}'

    cleanup:
    process.destroyForcibly()
  }

  def 'create mock server test'() {
    given:
    def pact = MainSpec.getResourceAsStream('/create-pact.json').text
    def process = invokeApp(true, '--daemon', '--debug', '31311')

    when:
    process.waitFor(2000, TimeUnit.MILLISECONDS)
    def result = createMock('31311', pact)

    then:
    result =~ /\{"port": \d+}/

    when:
    def result2 = getRoot('31311')

    then:
    result2 ==~ /\{"ports": \[\d+], "paths": \["\/data"]}/

    when:
    def mockJson = new JsonSlurper().parseText(result2)
    def result3 = getData(mockJson.ports[0])

    then:
    result3.code == 204

    when:
    result3 = getDataByPath('31311')

    then:
    result3.code == 204

    when:
    def result4 = complete('31311', mockJson.ports[0])

    then:
    result4 == ''

    when:
    def result5 = getRoot('31311')

    then:
    result5 == '{"ports": [], "paths": []}'

    cleanup:
    process.destroyForcibly()
  }

  Process invokeApp(boolean inheritIO = false, String... args) {
    def exec = System.getProperty('appExecutable')
    List<String> command = [exec]
    command.addAll(args)
    ProcessBuilder pb = new ProcessBuilder(command)
    if (inheritIO) {
      pb.inheritIO()
    }
    pb.start()
  }

  String getRoot(String port) {
    Request.get("http://127.0.0.1:$port/")
      .execute()
      .returnContent()
      .asString()
  }

  String createMock(String port, String pact) {
    Request.post("http://127.0.0.1:$port/create?state=any&path=%2Fdata")
      .bodyString(pact, ContentType.APPLICATION_JSON)
      .execute()
      .returnContent()
      .asString()
  }

  String complete(String port, mockPort) {
    Request.post("http://127.0.0.1:$port/complete")
      .bodyString("{\"port\":$mockPort}", ContentType.APPLICATION_JSON)
      .execute()
      .returnContent()
      .asString()
  }

  HttpResponse getData(port) {
    Request.get("http://127.0.0.1:$port/data")
      .execute()
      .returnResponse()
  }

  HttpResponse getDataByPath(port) {
    Request.get("http://127.0.0.1:$port/data")
      .execute()
      .returnResponse()
  }
}
