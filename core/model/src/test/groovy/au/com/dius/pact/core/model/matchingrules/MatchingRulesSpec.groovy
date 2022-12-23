package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

class MatchingRulesSpec extends Specification {

  def 'fromMap handles a null map'() {
    when:
    def matchingRules = MatchingRulesImpl.fromJson(null)

    then:
    matchingRules.empty
  }

  def 'fromMap handles an empty map'() {
    when:
    def matchingRules = MatchingRulesImpl.fromJson(new JsonValue.Object([:]))

    then:
    matchingRules.empty
  }

  def 'loads V2 matching rules'() {
    given:
    def matchingRulesMap = [
      '$.path': ['match': 'regex', 'regex': '\\w+'],
      '$.query.Q1': ['match': 'regex', 'regex': '\\d+'],
      '$.header.HEADERX': ['match': 'include', 'value': 'ValueA'],
      '$.headers.HEADERY': ['match': 'include', 'value': 'ValueA'],
      '$.body.animals': ['min': 1, 'match': 'type'],
      '$.body.animals[*].*': ['match': 'type'],
      '$.body.animals[*].children': ['min': 1],
      '$.body.animals[*].children[*].*': ['match': 'type']
    ]

    when:
    def matchingRules = MatchingRulesImpl.fromJson(Json.INSTANCE.toJson(matchingRulesMap))

    then:
    !matchingRules.empty
    matchingRules.categories == ['path', 'query', 'header', 'body'] as Set
    matchingRules.rulesForCategory('path') == new Category('path', [
      '': new MatchingRuleGroup([new RegexMatcher('\\w+') ]) ])
    matchingRules.rulesForCategory('query') == new Category('query', [
      Q1: new MatchingRuleGroup([ new RegexMatcher('\\d+') ]) ])
    matchingRules.rulesForCategory('header') == new Category('header', [
      HEADERX: new MatchingRuleGroup([ new IncludeMatcher('ValueA') ]),
      HEADERY: new MatchingRuleGroup([ new IncludeMatcher('ValueA') ]) ])
    matchingRules.rulesForCategory('body') == new Category('body', [
      '$.animals': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].*': new MatchingRuleGroup([TypeMatcher.INSTANCE ]),
      '$.animals[*].children': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].children[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ])
    ])
  }

  def 'loads V3 matching rules'() {
    given:
    def matchingRulesMap = [
      path: [
        'combine': 'OR',
        'matchers': [
          [ 'match': 'regex', 'regex': '\\w+' ]
        ]
      ],
      query: [
        'Q1': [
          'matchers': [
            [ 'match': 'regex', 'regex': '\\d+' ]
          ]
        ]
      ],
      header: [
        'HEADERY': [
          'combine': 'OR',
          'matchers': [
            ['match': 'include', 'value': 'ValueA'],
            ['match': 'include', 'value': 'ValueB']
          ]
        ]
      ],
      body: [
        '$.animals': [
          'matchers': [['min': 1, 'match': 'type']],
          'combine': 'OR'
        ],
        '$.animals[*].*': [
          'matchers': [['match': 'type']],
          'combine': 'AND',
        ],
        '$.animals[*].children': [
          'matchers': [['min': 1]],
          'combine': 'OTHER'
        ],
        '$.animals[*].children[*].*': [
          'matchers': [['match': 'type']]
        ]
      ]
    ]

    when:
    def matchingRules = MatchingRulesImpl.fromJson(Json.INSTANCE.toJson(matchingRulesMap))

    then:
    !matchingRules.empty
    matchingRules.categories == ['path', 'query', 'header', 'body'] as Set
    matchingRules.rulesForCategory('path') == new Category('path', [
      '': new MatchingRuleGroup([ new RegexMatcher('\\w+') ], RuleLogic.OR) ])
    matchingRules.rulesForCategory('query') == new Category('query', [
      Q1: new MatchingRuleGroup([ new RegexMatcher('\\d+') ]) ])
    matchingRules.rulesForCategory('header') == new Category('header', [
      HEADERY: new MatchingRuleGroup([ new IncludeMatcher('ValueA'), new IncludeMatcher('ValueB') ],
        RuleLogic.OR)
    ])
    matchingRules.rulesForCategory('body') == new Category('body', [
      '$.animals': new MatchingRuleGroup([ new MinTypeMatcher(1) ], RuleLogic.OR),
      '$.animals[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ]),
      '$.animals[*].children': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].children[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ])
    ])
  }

  @Unroll
  @SuppressWarnings('LineLength')
  def 'matchers fromJson returns #matcherClass.simpleName #condition'() {
    expect:
    MatchingRule.ruleFromMap(map).class == matcherClass

    where:
    map                                     | matcherClass                   | condition
    [:]                                     | EqualsMatcher                  | 'if the definition is empty'
    [other: 'value']                        | EqualsMatcher                  | 'if the definition is invalid'
    [match: 'something']                    | EqualsMatcher                  | 'if the matcher type is unknown'
    [match: 'equality']                     | EqualsMatcher                  | 'if the matcher type is equality'
    [match: 'regex', regex: '.*']           | RegexMatcher                   | 'if the matcher type is regex'
    [regex: '\\w+']                         | RegexMatcher                   | 'if the matcher definition contains a regex'
    [match: 'type']                         | TypeMatcher                    | 'if the matcher type is \'type\' and there is no min or max'
    [match: 'number']                       | NumberTypeMatcher              | 'if the matcher type is \'number\''
    [match: 'integer']                      | NumberTypeMatcher              | 'if the matcher type is \'integer\''
    [match: 'real']                         | NumberTypeMatcher              | 'if the matcher type is \'real\''
    [match: 'decimal']                      | NumberTypeMatcher              | 'if the matcher type is \'decimal\''
    [match: 'type', min: 1]                 | MinTypeMatcher                 | 'if the matcher type is \'type\' and there is a min'
    [match: 'min', min: 1]                  | MinTypeMatcher                 | 'if the matcher type is \'min\''
    [min: 1]                                | MinTypeMatcher                 | 'if the matcher definition contains a min'
    [match: 'type', max: 1]                 | MaxTypeMatcher                 | 'if the matcher type is \'type\' and there is a max'
    [match: 'max', max: 1]                  | MaxTypeMatcher                 | 'if the matcher type is \'max\''
    [max: 1]                                | MaxTypeMatcher                 | 'if the matcher definition contains a max'
    [match: 'type', max: 3, min: 2]         | MinMaxTypeMatcher              | 'if the matcher definition contains both a min and max'
    [match: 'timestamp']                    | TimestampMatcher               | 'if the matcher type is \'timestamp\''
    [timestamp: '1']                        | TimestampMatcher               | 'if the matcher definition contains a timestamp'
    [match: 'time']                         | TimeMatcher                    | 'if the matcher type is \'time\''
    [time: '1']                             | TimeMatcher                    | 'if the matcher definition contains a time'
    [match: 'date']                         | DateMatcher                    | 'if the matcher type is \'date\''
    [date: '1']                             | DateMatcher                    | 'if the matcher definition contains a date'
    [match: 'include', include: 'A']        | IncludeMatcher                 | 'if the matcher type is include'
    [match: 'values']                       | ValuesMatcher                  | 'if the matcher type is values'
  }

  @Issue('#743')
  def 'loads matching rules affected by defect #743'() {
    given:
    def matchingRulesMap = [
      'path': [
        '': [
          'matchers': [
            [ 'match': 'regex', 'regex': '\\w+' ]
          ]
        ]
      ]
    ]

    when:
    def matchingRules = MatchingRulesImpl.fromJson(Json.INSTANCE.toJson(matchingRulesMap))

    then:
    !matchingRules.empty
    matchingRules.categories == ['path'] as Set
    matchingRules.rulesForCategory('path') == new Category('path', [
      '': new MatchingRuleGroup([ new RegexMatcher('\\w+') ]) ])
  }

  @Issue('#743')
  def 'generates path matching rules in the correct format'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('path').addRule(new RegexMatcher('\\w+'))

    expect:
    matchingRules.toV3Map() == [path: [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']]
  }

  def 'do not include empty categories'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('path').addRule(new RegexMatcher('\\w+'))
    matchingRules.addCategory('body')
    matchingRules.addCategory('header')

    expect:
    matchingRules.toV3Map() == [path: [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']]
  }

  @Issue('#882')
  def 'With V2 format, matching rules for headers are pluralised'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('path').addRule(new RegexMatcher('\\w+'))
    matchingRules.addCategory('body')
    matchingRules.addCategory('header').addRule('X', new RegexMatcher('\\w+'))

    expect:
    matchingRules.toV2Map() == [
      '$.path': [match: 'regex', regex: '\\w+'],
      '$.headers.X': [match: 'regex', regex: '\\w+']
    ]
  }

  @Unroll
  def 'Loading Date/Time matchers'() {
    expect:
    MatchingRule.ruleFromMap(map) == matcher

    where:
    map                                           | matcher
    [match: 'timestamp']                          | new TimestampMatcher()
    [match: 'timestamp', timestamp: 'yyyy-MM-dd'] | new TimestampMatcher('yyyy-MM-dd')
    [match: 'timestamp', format: 'yyyy-MM-dd']    | new TimestampMatcher('yyyy-MM-dd')
    [match: 'date']                               | new DateMatcher()
    [match: 'date', date: 'yyyy-MM-dd']           | new DateMatcher('yyyy-MM-dd')
    [match: 'date', format: 'yyyy-MM-dd']         | new DateMatcher('yyyy-MM-dd')
    [match: 'time']                               | new TimeMatcher()
    [match: 'time', time: 'HH:mm']                | new TimeMatcher('HH:mm')
    [match: 'time', format: 'HH:mm']              | new TimeMatcher('HH:mm')
  }
}
