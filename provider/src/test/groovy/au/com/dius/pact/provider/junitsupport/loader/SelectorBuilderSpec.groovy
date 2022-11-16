package au.com.dius.pact.provider.junitsupport.loader

import spock.lang.Specification

import static au.com.dius.pact.core.support.JsonKt.jsonArray

class SelectorBuilderSpec extends Specification {

  def 'allow providing selectors in raw form'() {
    expect:
    jsonArray(new SelectorBuilder()
      .rawSelectorJson('{"iAmA": "selector"}')
      .build()
      *.toJson())
      .serialise() == '[{"iAmA":"selector"}]'
  }

}
