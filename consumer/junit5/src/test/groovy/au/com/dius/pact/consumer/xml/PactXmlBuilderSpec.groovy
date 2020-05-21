package au.com.dius.pact.consumer.xml

import spock.lang.Ignore
import spock.lang.Specification

import static au.com.dius.pact.consumer.dsl.Matchers.integer
import static au.com.dius.pact.consumer.dsl.Matchers.string

class PactXmlBuilderSpec extends Specification {
  @Ignore // fails on travis due to whitespace differences
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
    def result = builder.toString()

    then:
    result == '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
      |<projects id="1234">
      |<project id="12" name=" Project 1 " type="activity"/>
      |<project id="12" name=" Project 1 " type="activity"/>
      |</projects>
      |'''.stripMargin()
  }
}
