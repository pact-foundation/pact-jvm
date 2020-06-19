package au.com.dius.pact.consumer.xml

import groovy.xml.XmlSlurper
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.Matchers.integer
import static au.com.dius.pact.consumer.dsl.Matchers.string

class PactXmlBuilderSpec extends Specification {
  def 'without a namespace'() {
    given:
    def builder = new PactXmlBuilder('projects').build { root ->
      root.setAttributes([id: '1234'])
      root.eachLike('project', 2, [
        id: integer(12),
        type: 'activity',
        name: string(' Project 1 ')
      ])
    }

    when:
    def result = new XmlSlurper().parseText(builder.toString())

    then:
    result.@id == '1234'
    result.project.size() == 2
    result.project.each {
      assert it.@id == '12'
      assert it.@name == ' Project 1 '
      assert it.@type == 'activity'
    }
  }

  def 'elements with mutiple different types'() {
    given:
    def builder = new PactXmlBuilder('animals').build { root ->
      root.eachLike('dog', 2, [
        id: integer(1),
        name: string('Canine')
      ])
      root.eachLike('cat', 3, [
        id: integer(2),
        name: string('Feline')
      ])
      root.eachLike('wolf', 1, [
        id: integer(3),
        name: string('Canine')
      ])
    }

    when:
    def result = new XmlSlurper().parseText(builder.toString())

    then:
    result.dog.size() == 2
    result.cat.size() == 3
    result.wolf.size() == 1
  }
}
