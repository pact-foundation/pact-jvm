package au.com.dius.pact.provider.gradle

import au.com.dius.pact.core.pactbroker.ConsumerVersionSelectors
import spock.lang.Specification
import spock.lang.Unroll

class ConsumerVersionSelectorConfigSpec extends Specification {
  ConsumerVersionSelectorConfig config

  def setup() {
    config = new ConsumerVersionSelectorConfig()
  }

  def 'main branch selector'() {
    when:
    config.mainBranch()

    then:
    config.selectors == [ ConsumerVersionSelectors.MainBranch.INSTANCE ]
  }

  @Unroll
  def 'branch selector'() {
    when:
    config.branch(name, consumer, fallback)

    then:
    config.selectors == [ selector ]

    where:

    name   | consumer | fallback | selector
    'test' | null     | null     | new ConsumerVersionSelectors.Branch('test')
    'test' | 'con'    | null     | new ConsumerVersionSelectors.Branch('test', 'con')
    'test' | null     | 'back'   | new ConsumerVersionSelectors.Branch('test', null, 'back')
    'test' | 'con'    | 'back'   | new ConsumerVersionSelectors.Branch('test', 'con', 'back')
  }

  def 'deployed or released selector'() {
    when:
    config.deployedOrReleased()

    then:
    config.selectors == [ ConsumerVersionSelectors.DeployedOrReleased.INSTANCE ]
  }

  def 'matching branch selector'() {
    when:
    config.matchingBranch()

    then:
    config.selectors == [ ConsumerVersionSelectors.MatchingBranch.INSTANCE ]
  }

  def 'deployed to selector'() {
    when:
    config.deployedTo('env')

    then:
    config.selectors == [ new ConsumerVersionSelectors.DeployedTo('env') ]
  }

  def 'released to selector'() {
    when:
    config.releasedTo('env')

    then:
    config.selectors == [ new ConsumerVersionSelectors.ReleasedTo('env') ]
  }

  def 'environment selector'() {
    when:
    config.environment('env')

    then:
    config.selectors == [ new ConsumerVersionSelectors.Environment('env') ]
  }

  def 'tag selector'() {
    when:
    config.tag('t')

    then:
    config.selectors == [ new ConsumerVersionSelectors.Tag('t') ]
  }

  def 'latest tag selector'() {
    when:
    config.latestTag('t')

    then:
    config.selectors == [ new ConsumerVersionSelectors.LatestTag('t') ]
  }

  @Unroll
  def 'generic selector'() {
    when:
    config.selector(tag, latest, fallback, consumer)

    then:
    config.selectors == [ selector ]

    where:

    tag    | latest | consumer | fallback | selector
    'test' | null   | null     | null     | new ConsumerVersionSelectors.Selector('test')
    'test' | true   | null     | null     | new ConsumerVersionSelectors.Selector('test', true)
    'test' | true   | null     | 'back'   | new ConsumerVersionSelectors.Selector('test', true, null, 'back')
    'test' | true   | 'con'    | 'back'   | new ConsumerVersionSelectors.Selector('test', true, 'con', 'back')
    null   | true   | null     | null     | new ConsumerVersionSelectors.Selector(null, true, null, null)
  }
}
