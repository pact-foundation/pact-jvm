package au.com.dius.pact.core.model.matchingrules

import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.support.Json
import au.com.dius.pact.core.support.json.JsonValue
import kotlin.Triple
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

@SuppressWarnings('LineLength')
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
    matchingRules.rulesForCategory('path') == new MatchingRuleCategory('path', [
      '': new MatchingRuleGroup([new RegexMatcher('\\w+') ]) ])
    matchingRules.rulesForCategory('query') == new MatchingRuleCategory('query', [
      Q1: new MatchingRuleGroup([ new RegexMatcher('\\d+') ]) ])
    matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header', [
      HEADERX: new MatchingRuleGroup([ new IncludeMatcher('ValueA') ]),
      HEADERY: new MatchingRuleGroup([ new IncludeMatcher('ValueA') ]) ])
    matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body', [
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
    matchingRules.rulesForCategory('path') == new MatchingRuleCategory('path', [
      '': new MatchingRuleGroup([ new RegexMatcher('\\w+') ], RuleLogic.OR) ])
    matchingRules.rulesForCategory('query') == new MatchingRuleCategory('query', [
      Q1: new MatchingRuleGroup([ new RegexMatcher('\\d+') ]) ])
    matchingRules.rulesForCategory('header') == new MatchingRuleCategory('header', [
      HEADERY: new MatchingRuleGroup([ new IncludeMatcher('ValueA'), new IncludeMatcher('ValueB') ],
        RuleLogic.OR)
    ])
    matchingRules.rulesForCategory('body') == new MatchingRuleCategory('body', [
      '$.animals': new MatchingRuleGroup([ new MinTypeMatcher(1) ], RuleLogic.OR),
      '$.animals[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ]),
      '$.animals[*].children': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].children[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ])
    ])
  }

  @Unroll
  def 'matchers fromJson returns #matcherClass.simpleName #condition'() {
    expect:
    MatchingRule.fromJson(Json.toJson(map)).class == matcherClass

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
    [match: 'ignore-order']                 | EqualsIgnoreOrderMatcher       | 'if the matcher type is \'ignore-order\' and there is no min or max'
    [match: 'ignore-order', min: 1]         | MinEqualsIgnoreOrderMatcher    | 'if the matcher type is \'ignore-order\' and there is a min'
    [match: 'ignore-order', max: 1]         | MaxEqualsIgnoreOrderMatcher    | 'if the matcher type is \'ignore-order\' and there is a max'
    [match: 'ignore-order', max: 3, min: 2] | MinMaxEqualsIgnoreOrderMatcher | 'if the matcher type is \'ignore-order\' and there is a min and max'
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
    matchingRules.rulesForCategory('path') == new MatchingRuleCategory('path', [
      '': new MatchingRuleGroup([ new RegexMatcher('\\w+') ]) ])
  }

  @Issue('#743')
  def 'generates path matching rules in the correct format'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('path').addRule(new RegexMatcher('\\w+'))

    expect:
    matchingRules.toV3Map(PactSpecVersion.V3) == [path: [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']]
  }

  def 'do not include empty categories'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('path').addRule(new RegexMatcher('\\w+'))
    matchingRules.addCategory('body')
    matchingRules.addCategory('header')

    expect:
    matchingRules.toV3Map(PactSpecVersion.V3) == [path: [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']]
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

  def 'Array contains matcher to map for JSON'() {
    expect:
    new ArrayContainsMatcher([ new Triple(0, new MatchingRuleCategory('Variant 1', [
      '$.index': new MatchingRuleGroup([new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER)])
    ]), [:])]).toMap(PactSpecVersion.V4) == [
      match: 'arrayContains',
      variants: [
        [
          index: 0,
          rules: [
          '$.index': [matchers: [[match: 'integer']], combine: 'AND']
          ],
          generators: [:]
        ]
      ]
    ]
  }

  def 'Load array contains matcher from json'() {
    given:
    def matchingRulesMap = [
      body: [
        '$': [
          matchers: [
            [
              match: 'arrayContains',
              variants: [
                [
                  index: 0,
                  rules: [
                    '$.href': [
                      combine: 'AND',
                      matchers: [
                        [
                          match: 'regex',
                          regex: '.*\\/orders\\/\\d+$'
                        ]
                      ]
                    ]
                  ]
                ]
              ]
            ]
          ]
        ]
      ]
    ]

    when:
    def matchingRules = MatchingRulesImpl.fromJson(Json.INSTANCE.toJson(matchingRulesMap))

    then:
    !matchingRules.empty
    matchingRules.categories == ['body'] as Set
    matchingRules.rulesForCategory('body').matchingRules.size() == 1
    matchingRules.rulesForCategory('body').matchingRules['$'] == new MatchingRuleGroup([
      new ArrayContainsMatcher([
        new Triple(0, new MatchingRuleCategory('body', [
          '$.href': new MatchingRuleGroup([new RegexMatcher('.*\\/orders\\/\\d+$')])
        ]), [:])
      ])]
    )
  }

  def 'renaming categories'() {
    given:
    def matchingRules = new MatchingRulesImpl()
    matchingRules.addCategory('path').addRule(new RegexMatcher('\\w+'))
    matchingRules.addCategory('body')
    matchingRules.addCategory('header')

    expect:
    matchingRules.rename('path', 'content').toV3Map(PactSpecVersion.V3) == [
      content: [matchers: [[match: 'regex', regex: '\\w+']], combine: 'AND']
    ]
  }

  def 'status code matcher to map for JSON'() {
    expect:
    new StatusCodeMatcher(HttpStatus.ClientError, []).toMap(PactSpecVersion.V4) == [
      match: 'statusCode', status: 'clientError'
    ]
    new StatusCodeMatcher(HttpStatus.StatusCodes, [501, 503]).toMap(PactSpecVersion.V4) == [
      match: 'statusCode',
      status: [501, 503]
    ]
  }

  def 'Load status code matcher from json'() {
    given:
    def matchingRulesMap = [
      body: [
        '$': [
          matchers: [
            [
              match: 'statusCode',
              status: 'redirect'
            ],
            [
              match: 'statusCode',
              status: [100, 200]
            ]
          ]
        ]
      ]
    ]

    when:
    def matchingRules = MatchingRulesImpl.fromJson(Json.INSTANCE.toJson(matchingRulesMap))

    then:
    !matchingRules.empty
    matchingRules.categories == ['body'] as Set
    matchingRules.rulesForCategory('body').matchingRules.size() == 1
    matchingRules.rulesForCategory('body').matchingRules['$'] == new MatchingRuleGroup([
      new StatusCodeMatcher(HttpStatus.Redirect, []),
      new StatusCodeMatcher(HttpStatus.StatusCodes, [100, 200])
    ])
  }
}
