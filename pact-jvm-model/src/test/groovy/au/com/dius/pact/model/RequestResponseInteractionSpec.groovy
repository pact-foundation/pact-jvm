package au.com.dius.pact.model

import spock.lang.Specification

class RequestResponseInteractionSpec extends Specification {

  def 'unique key test'() {
    expect:
    interaction1.uniqueKey() == interaction1.uniqueKey()
    interaction1.uniqueKey() == interaction2.uniqueKey()
    interaction1.uniqueKey() != interaction3.uniqueKey()
    interaction1.uniqueKey() != interaction4.uniqueKey()
    interaction1.uniqueKey() != interaction5.uniqueKey()
    interaction3.uniqueKey() != interaction4.uniqueKey()
    interaction3.uniqueKey() != interaction5.uniqueKey()
    interaction4.uniqueKey() != interaction5.uniqueKey()

    where:
    interaction1 = new RequestResponseInteraction('description 1+2')
    interaction2 = new RequestResponseInteraction('description 1+2')
    interaction3 = new RequestResponseInteraction('description 1+2', 'state 3')
    interaction4 = new RequestResponseInteraction('description 4')
    interaction5 = new RequestResponseInteraction('description 4', 'state 5')
  }

}
