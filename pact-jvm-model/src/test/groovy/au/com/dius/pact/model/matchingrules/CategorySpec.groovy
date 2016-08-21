package au.com.dius.pact.model.matchingrules

import spock.lang.Specification

class CategorySpec extends Specification {

  def 'defaults to AND for combining rules'() {
    expect:
    new Category().ruleLogic == RuleLogic.AND
  }

}
