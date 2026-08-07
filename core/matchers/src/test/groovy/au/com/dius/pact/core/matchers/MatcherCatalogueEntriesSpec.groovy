package au.com.dius.pact.core.matchers

import au.com.dius.pact.core.model.matchingrules.ArrayContainsMatcher
import au.com.dius.pact.core.model.matchingrules.BooleanMatcher
import au.com.dius.pact.core.model.matchingrules.ContentTypeMatcher
import au.com.dius.pact.core.model.matchingrules.DateMatcher
import au.com.dius.pact.core.model.matchingrules.EachKeyMatcher
import au.com.dius.pact.core.model.matchingrules.EachValueMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.EqualsMatcher
import au.com.dius.pact.core.model.matchingrules.HttpStatus
import au.com.dius.pact.core.model.matchingrules.IncludeMatcher
import au.com.dius.pact.core.model.matchingrules.MaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxEqualsIgnoreOrderMatcher
import au.com.dius.pact.core.model.matchingrules.MinMaxTypeMatcher
import au.com.dius.pact.core.model.matchingrules.MinTypeMatcher
import au.com.dius.pact.core.model.matchingrules.NotEmptyMatcher
import au.com.dius.pact.core.model.matchingrules.NullMatcher
import au.com.dius.pact.core.model.matchingrules.NumberTypeMatcher
import au.com.dius.pact.core.model.matchingrules.RegexMatcher
import au.com.dius.pact.core.model.matchingrules.SemverMatcher
import au.com.dius.pact.core.model.matchingrules.StatusCodeMatcher
import au.com.dius.pact.core.model.matchingrules.TimeMatcher
import au.com.dius.pact.core.model.matchingrules.TimestampMatcher
import au.com.dius.pact.core.model.matchingrules.TypeMatcher
import au.com.dius.pact.core.model.matchingrules.ValuesMatcher
import au.com.dius.pact.core.model.matchingrules.expressions.MatchingRuleDefinition
import au.com.dius.pact.core.model.generators.ArrayContainsGenerator
import au.com.dius.pact.core.model.generators.DateGenerator
import au.com.dius.pact.core.model.generators.DateTimeGenerator
import au.com.dius.pact.core.model.generators.MockServerURLGenerator
import au.com.dius.pact.core.model.generators.NullGenerator
import au.com.dius.pact.core.model.generators.ProviderStateGenerator
import au.com.dius.pact.core.model.generators.RandomBooleanGenerator
import au.com.dius.pact.core.model.generators.RandomDecimalGenerator
import au.com.dius.pact.core.model.generators.RandomHexadecimalGenerator
import au.com.dius.pact.core.model.generators.RandomIntGenerator
import au.com.dius.pact.core.model.generators.RandomStringGenerator
import au.com.dius.pact.core.model.generators.RegexGenerator
import au.com.dius.pact.core.model.generators.TimeGenerator
import au.com.dius.pact.core.model.generators.UuidGenerator
import spock.lang.Specification

/**
 * The catalogue entry keys are the rule and generator names, so a plugin can name one it was handed
 * when it calls back into the host. Adding a matching rule or generator without a catalogue entry
 * would leave it unreachable from a plugin, so this pins the lists together.
 */
class MatcherCatalogueEntriesSpec extends Specification {

  private static final List RULES = [
    EqualsMatcher.INSTANCE,
    new RegexMatcher('.*'),
    TypeMatcher.INSTANCE,
    new MinTypeMatcher(1),
    new MaxTypeMatcher(1),
    new MinMaxTypeMatcher(1, 2),
    new IncludeMatcher('a'),
    new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER),
    new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER),
    new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL),
    BooleanMatcher.INSTANCE,
    NullMatcher.INSTANCE,
    new DateMatcher(),
    new TimeMatcher(),
    new TimestampMatcher(),
    new ContentTypeMatcher('text/plain'),
    ValuesMatcher.INSTANCE,
    new ArrayContainsMatcher([]),
    EqualsIgnoreOrderMatcher.INSTANCE,
    new MinEqualsIgnoreOrderMatcher(1),
    new MaxEqualsIgnoreOrderMatcher(1),
    new MinMaxEqualsIgnoreOrderMatcher(1, 2),
    new StatusCodeMatcher(HttpStatus.Success, []),
    NotEmptyMatcher.INSTANCE,
    SemverMatcher.INSTANCE,
    new EachKeyMatcher(new MatchingRuleDefinition('a', TypeMatcher.INSTANCE, null, '')),
    new EachValueMatcher(new MatchingRuleDefinition('a', TypeMatcher.INSTANCE, null, ''))
  ]

  def 'every matching rule has a catalogue entry keyed by its name'() {
    given:
    def entryKeys = MatcherExecutorKt.matcherCatalogueEntries()*.key as Set
    def ruleNames = RULES*.name as Set

    expect:
    ruleNames - entryKeys == [] as Set
    entryKeys - ruleNames == [] as Set
  }

  private static final List GENERATORS = [
    new RandomIntGenerator(0, 10),
    new RandomDecimalGenerator(4),
    new RandomHexadecimalGenerator(4),
    new RandomStringGenerator(4),
    RandomBooleanGenerator.INSTANCE,
    new RegexGenerator('.*'),
    new UuidGenerator(),
    new DateGenerator(),
    new TimeGenerator(),
    new DateTimeGenerator(),
    new ProviderStateGenerator('a'),
    new MockServerURLGenerator('http://localhost:8080', '.*'),
    new ArrayContainsGenerator([]),
    NullGenerator.INSTANCE
  ]

  def 'every generator has a catalogue entry keyed by its name'() {
    given:
    def entryKeys = MatcherExecutorKt.generatorCatalogueEntries()*.key as Set
    def generatorNames = GENERATORS*.type as Set

    expect:
    generatorNames - entryKeys == [] as Set
    entryKeys - generatorNames == [] as Set
  }

  def 'every catalogue entry records the specification version it was introduced in'() {
    given:
    def entries = MatcherExecutorKt.matcherCatalogueEntries() + MatcherExecutorKt.generatorCatalogueEntries()

    expect:
    entries.every {
      it.values['spec-version'] in ['V1', 'V2', 'V3', 'V4']
    }
  }
}
