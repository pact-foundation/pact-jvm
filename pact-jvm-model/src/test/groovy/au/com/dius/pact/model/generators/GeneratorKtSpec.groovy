package au.com.dius.pact.model.generators

import spock.lang.Specification
import spock.lang.Unroll
import spock.util.environment.RestoreSystemProperties

class GeneratorKtSpec extends Specification {

  @Unroll
  @RestoreSystemProperties
  def 'find generators looks for the generator in the pact.generators.packages system property'() {
    setup:
    System.setProperty('pact.generators.packages', [
      'au.com.dius.pact.model.generators.test.pkg1',
      'au.com.dius.pact.model.generators.test.pkg2',
      'au.com.dius.pact.model.generators.test.pkg3'
    ].join(','))

    expect:
    GeneratorKt.findGeneratorClass(type).name == generatorClass

    where:

    type   | generatorClass
    'Pkg1' | 'au.com.dius.pact.model.generators.test.pkg1.Pkg1Generator'
    'Pkg2' | 'au.com.dius.pact.model.generators.test.pkg2.Pkg2Generator'
    'Pkg3' | 'au.com.dius.pact.model.generators.test.pkg3.Pkg3Generator'
  }

  @Unroll
  @RestoreSystemProperties
  def 'find generators defaults to the generators model package if not found'() {
    given:
    if (packages != null) {
      System.setProperty('pact.generators.packages', packages)
    }

    expect:
    GeneratorKt.findGeneratorClass('Date').name == 'au.com.dius.pact.model.generators.DateGenerator'

    where:

    packages << [null, '', 'au.com.dius.pact.model.generators.test.pkgX', 'au.com.dius.pact.model.generators.test.pkg1']
  }

  @RestoreSystemProperties
  def 'throws a class not found exception if the generator was not found'() {
    setup:
    System.setProperty('pact.generators.packages', [
      'au.com.dius.pact.model.generators.test.pkg1',
      'au.com.dius.pact.model.generators.test.pkg2',
      'au.com.dius.pact.model.generators.test.pkg3'
    ].join(','))

    when:
    GeneratorKt.findGeneratorClass('IShouldReallyNotExist')

    then:
    thrown(ClassNotFoundException)
  }

}
