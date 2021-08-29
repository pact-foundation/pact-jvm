package au.com.dius.pact.consumer.xml

import au.com.dius.pact.core.model.generators.Category
import groovy.xml.XmlSlurper
import spock.lang.Specification
import spock.lang.Unroll

import static au.com.dius.pact.consumer.dsl.Matchers.bool
import static au.com.dius.pact.consumer.dsl.Matchers.integer
import static au.com.dius.pact.consumer.dsl.Matchers.string
import static au.com.dius.pact.consumer.dsl.Matchers.timestamp

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

  def 'matching rules'() {
    given:
    def builder = new PactXmlBuilder('projects', 'http://some.namespace/and/more/stuff')
      .build { root ->
        root.setAttributes([id: '1234'])
        root.eachLike('project', 1, [
          id: integer(),
          type: 'activity',
          name: string('Project 1'),
          due: timestamp("yyyy-MM-dd'T'HH:mm:ss.SSSX", '2016-02-11T09:46:56.023Z')
        ]) { project ->
          project.appendElement('tasks', [:]) { task ->
            task.eachLike('task', 1, [id: integer(), name: string('Task 1'), done: bool(true)])
          }
        }
      }

    when:
    def xml = new XmlSlurper().parseText(builder.toString())
    def matchers = builder.matchingRules
    def generators = builder.generators

    then:
    xml.@id == '1234'
    matchers.matchingRules.keySet() == [
      "\$.ns:projects.project",
      "\$.ns:projects.project['@id']",
      "\$.ns:projects.project['@name']",
      "\$.ns:projects.project['@due']",
      "\$.ns:projects.project.tasks.task",
      "\$.ns:projects.project.tasks.task['@id']",
      "\$.ns:projects.project.tasks.task['@name']",
      "\$.ns:projects.project.tasks.task['@done']"
    ] as Set
    generators.categoryFor(Category.BODY).keySet() == [
      "\$.ns:projects.project['@id']",
      "\$.ns:projects.project.tasks.task['@id']"
    ] as Set
  }

  @Unroll
  def 'matcher key path'() {
    expect:
    PactXmlBuilderKt.matcherKey(base, *keys) == path

    where:

    base         | keys              || path
    ['$']        | []                 | '$'
    ['$', 'one'] | []                 | '$.one'
    ['$', 'one'] | ['two']            | '$.one.two'
    ['$', 'one'] | ['two', "['@id']"] | "\$.one.two['@id']"
    ['$', 'one'] | ['two', '#text']   | "\$.one.two.#text"
  }

  @Unroll
  def 'standalone declaration - #standalone'() {
    given:
    def builder = new PactXmlBuilder('projects')
      .withStandalone(standalone)
      .build { node ->
        node.setAttributes([id: '1234'])
      }

    when:
    def result = builder.toString()

    then:
    result.startsWith(value)

    where:

    standalone | value
    true       | '<?xml version="1.0" encoding="UTF-8"?>'
    false      | '<?xml version="1.0" encoding="UTF-8" standalone="no"?>'
  }
}
