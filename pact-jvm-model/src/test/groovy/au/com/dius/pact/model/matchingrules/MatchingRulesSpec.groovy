package au.com.dius.pact.model.matchingrules

import spock.lang.Specification

class MatchingRulesSpec extends Specification {

  def 'fromMap handles a null map'() {
    when:
    def matchingRules = MatchingRulesImpl.fromMap(null)

    then:
    matchingRules.empty
  }

  def 'fromMap handles an empty map'() {
    when:
    def matchingRules = MatchingRulesImpl.fromMap([:])

    then:
    matchingRules.empty
  }

  def 'loads V2 matching rules'() {
    given:
    def matchingRulesMap = [
      '$.path': ['match': 'regex', 'regex': '\\w+'],
      '$.query.Q1': ['match': 'regex', 'regex': '\\d+'],
      '$.header.HEADERY': ['match': 'include', 'value': 'ValueA'],
      '$.body.animals': ['min': 1, 'match': 'type'],
      '$.body.animals[*].*': ['match': 'type'],
      '$.body.animals[*].children': ['min': 1],
      '$.body.animals[*].children[*].*': ['match': 'type']
    ]

    when:
    def matchingRules = MatchingRulesImpl.fromMap(matchingRulesMap)

    then:
    !matchingRules.empty
    matchingRules.categories == ['path', 'query', 'header', 'body'] as Set
    matchingRules.rulesForCategory('path') == new Category('path', [
      '': new MatchingRuleGroup([ new RegexMatcher('\\w+') ]) ])
    matchingRules.rulesForCategory('query') == new Category('query', [
      Q1: new MatchingRuleGroup([ new RegexMatcher('\\d+') ]) ])
    matchingRules.rulesForCategory('header') == new Category('header', [
      HEADERY: new MatchingRuleGroup([ new IncludeMatcher('ValueA') ]) ])
    matchingRules.rulesForCategory('body') == new Category('body', [
      '$.animals': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ]),
      '$.animals[*].children': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].children[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ])
    ])
  }

  def 'loads V3 matching rules'() {
    given:
    def matchingRulesMap = [
      'path': [
        'matchers': [
          [ 'match': 'regex', 'regex': '\\w+' ]
        ]
      ],
      'query': [
        'Q1': [
          'matchers': [
            [ 'match': 'regex', 'regex': '\\d+' ]
          ]
        ]
      ],
      'header': [
        'HEADERY': [
          'combine': 'AND',
          'matchers': [
            ['match': 'include', 'value': 'ValueA'],
            ['match': 'include', 'value': 'ValueB']
          ]
        ]
      ],
      'body': [
        '$.animals': [
          'matchers': [['min': 1, 'match': 'type']]
        ],
        '$.animals[*].*': [
          'matchers': [['match': 'type']]
        ],
        '$.animals[*].children': [
          'matchers': [['min': 1]]
        ],
        '$.animals[*].children[*].*': [
          'matchers': [['match': 'type']]
        ]
      ]
    ]

    when:
    def matchingRules = MatchingRulesImpl.fromMap(matchingRulesMap)

    then:
    !matchingRules.empty
    matchingRules.categories == ['path', 'query', 'header', 'body'] as Set
    matchingRules.rulesForCategory('path') == new Category('path', [
      '': new MatchingRuleGroup([ new RegexMatcher('\\w+') ]) ])
    matchingRules.rulesForCategory('query') == new Category('query', [
      Q1: new MatchingRuleGroup([ new RegexMatcher('\\d+') ]) ])
    matchingRules.rulesForCategory('header') == new Category('header', [
      HEADERY: new MatchingRuleGroup([ new IncludeMatcher('ValueA'), new IncludeMatcher('ValueB') ])
    ])
    matchingRules.rulesForCategory('body') == new Category('body', [
      '$.animals': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ]),
      '$.animals[*].children': new MatchingRuleGroup([ new MinTypeMatcher(1) ]),
      '$.animals[*].children[*].*': new MatchingRuleGroup([ TypeMatcher.INSTANCE ])
    ])
  }

}
