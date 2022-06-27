package au.com.dius.pact.core.pactbroker

import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
class ConsumerVersionSelectorsSpec extends Specification {
  @Unroll
  def 'convert to JSON'() {
    expect:
    selector.toJson().serialise() == json

    where:

    selector                                                                 | json
    ConsumerVersionSelectors.MainBranch.INSTANCE                             | '{"mainBranch":true}'
    new ConsumerVersionSelectors.Branch('<branch>')                          | '{"branch":"<branch>"}'
    ConsumerVersionSelectors.DeployedOrReleased.INSTANCE                     | '{"deployedOrReleased":true}'
    ConsumerVersionSelectors.MatchingBranch.INSTANCE                         | '{"matchingBranch":true}'
    new ConsumerVersionSelectors.Branch('<branch>', '<consumer>')            | '{"branch":"<branch>","consumer":"<consumer>"}'
    new ConsumerVersionSelectors.Branch('<branch>', null, '<fbranch>')       | '{"branch":"<branch>","fallbackBranch":"<fbranch>"}'
    new ConsumerVersionSelectors.DeployedTo('<environment>')                 | '{"deployed":true,"environment":"<environment>"}'
    new ConsumerVersionSelectors.ReleasedTo('<environment>')                 | '{"environment":"<environment>","released":true}'
    new ConsumerVersionSelectors.Environment('<environment>')                | '{"environment":"<environment>"}'
    new ConsumerVersionSelectors.Tag('<tag>')                                | '{"tag":"<tag>"}'
    new ConsumerVersionSelectors.LatestTag('<tag>')                          | '{"latest":true,"tag":"<tag>"}'
    new ConsumerVersionSelectors.LatestTag('<tag>', '<fallback>')            | '{"fallbackTag":"<fallback>","latest":true,"tag":"<tag>"}'
    new ConsumerVersionSelectors.Selector('<tag>')                           | '{"tag":"<tag>"}'
    new ConsumerVersionSelectors.Selector('<tag>', true)                     | '{"latest":true,"tag":"<tag>"}'
    new ConsumerVersionSelectors.Selector('<tag>', false)                    | '{"latest":false,"tag":"<tag>"}'
    new ConsumerVersionSelectors.Selector('<tag>', true, null, '<fallback>') | '{"fallbackTag":"<fallback>","latest":true,"tag":"<tag>"}'
    new ConsumerVersionSelectors.Selector('<tag>', true, '<consumer>')       | '{"consumer":"<consumer>","latest":true,"tag":"<tag>"}'
    new ConsumerVersionSelectors.Selector(null, true)                        | '{"latest":true}'
  }
}
