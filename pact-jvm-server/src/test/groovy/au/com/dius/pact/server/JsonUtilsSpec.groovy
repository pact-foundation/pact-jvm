package au.com.dius.pact.server

import scala.collection.JavaConversions
import spock.lang.Specification

class JsonUtilsSpec extends Specification {

  def "Parsing JSON bodies - handles a normal JSON body"() {
    expect:
    JavaConversions.mapAsJavaMap(JsonUtils.parseJsonString(
      '{"password":"123456","firstname":"Brent","booleam":"true","username":"bbarke","lastname":"Barker"}'
    )) == [username: 'bbarke', firstname: 'Brent', lastname: 'Barker', booleam: 'true', password: '123456']
  }

  def "Parsing JSON bodies - handles a String"() {
    expect:
    JsonUtils.parseJsonString('"I am a string"') == 'I am a string'
  }

  def "Parsing JSON bodies - handles a Number"() {
    expect:
    JsonUtils.parseJsonString('1234') == 1234
  }

  def "Parsing JSON bodies - handles a Boolean"() {
    expect:
    JsonUtils.parseJsonString('true') == true
  }

  def "Parsing JSON bodies - handles a Null"() {
    expect:
    JsonUtils.parseJsonString('null') == null
  }

  def "Parsing JSON bodies - handles an array"() {
    expect:
    JavaConversions.seqAsJavaList(JsonUtils.parseJsonString('[1, 2, 3, 4]').toSeq()) == [1, 2, 3, 4]
  }

  def "Parsing JSON bodies - handles an empty body"() {
    expect:
    JsonUtils.parseJsonString('') == null
  }

}
