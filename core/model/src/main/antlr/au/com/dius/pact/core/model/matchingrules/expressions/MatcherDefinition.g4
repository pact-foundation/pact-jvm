grammar MatcherDefinition;

@header {
    package au.com.dius.pact.core.model.matchingrules.expressions;

    import au.com.dius.pact.core.model.matchingrules.*;
    import au.com.dius.pact.core.model.generators.Generator;
}

/**
* Parse a matcher expression into a MatchingRuleDefinition containing the example value, matching rules and any generator.
* The following are examples of matching rule definitions:
* * `matching(type,'Name')` - type matcher
* * `matching(number,100)` - number matcher
* * `matching(datetime, 'yyyy-MM-dd','2000-01-01')` - datetime matcher with format string
**/
matchingDefinition returns [ MatchingRuleDefinition value ] :
    matchingDefinitionExp { $value = $matchingDefinitionExp.value; } ( COMMA e=matchingDefinitionExp {  if ($value != null) { $value = $value.merge($e.value); } } )* EOF
    ;

matchingDefinitionExp returns [ MatchingRuleDefinition value ] :
    (
      'matching' LEFT_BRACKET matchingRule RIGHT_BRACKET { $value = new MatchingRuleDefinition($matchingRule.value, $matchingRule.rule, $matchingRule.generator); }
      | 'notEmpty' LEFT_BRACKET string RIGHT_BRACKET { $value = new MatchingRuleDefinition($string.contents, NotEmptyMatcher.INSTANCE, null); }
      | 'eachKey' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET { if ($e.value != null) { $value = new MatchingRuleDefinition(null, new EachKeyMatcher($e.value), null); } }
      | 'eachValue' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET { if ($e.value != null) { $value = new MatchingRuleDefinition(null, new EachValueMatcher($e.value), null); } }
    )
    ;

matchingRule returns [ String value, MatchingRule rule, Generator generator ] :
  (
    ( 'equalTo' { $rule = EqualsMatcher.INSTANCE; }
    | 'type'  { $rule = TypeMatcher.INSTANCE; } )
    COMMA string { $value = $string.contents; } )
  | 'number' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER); } COMMA val=( DECIMAL_LITERAL | INTEGER_LITERAL ) { $value = $val.getText(); }
  | 'integer' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER); } COMMA val=INTEGER_LITERAL { $value = $val.getText(); }
  | 'decimal' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL); } COMMA val=DECIMAL_LITERAL { $value = $val.getText(); }
  | matcherType=( 'datetime' | 'date' | 'time' ) COMMA format=string {
    if ($matcherType.getText().equals("datetime")) { $rule = new TimestampMatcher($format.contents); }
    if ($matcherType.getText().equals("date")) { $rule = new DateMatcher($format.contents); }
    if ($matcherType.getText().equals("time")) { $rule = new TimeMatcher($format.contents); }
    } COMMA v=string { $value = $v.contents; }
  | 'regex' COMMA r=string COMMA v=string { $rule = new RegexMatcher($r.contents); $value = $v.contents; }
  | 'include' COMMA v=string { $rule = new IncludeMatcher($v.contents); $value = $v.contents; }
  | 'boolean' COMMA BOOLEAN_LITERAL { $rule = BooleanMatcher.INSTANCE; $value = $BOOLEAN_LITERAL.getText(); }
  | 'semver' COMMA v=string { $rule = SemverMatcher.INSTANCE; $value = $value = $v.contents; }
  | 'contentType' COMMA ct=string COMMA v=string { $rule = new ContentTypeMatcher($ct.contents); $value = $v.contents; }
  ;

string returns [ String contents ] :
  STRING_LITERAL {
    String contents = $STRING_LITERAL.getText();
    $contents = contents.substring(1, contents.length() - 1);
  }
  | 'null'
  ;

INTEGER_LITERAL : '-'? DIGIT+ ;
DECIMAL_LITERAL : '-'? DIGIT+ '.' DIGIT+ ;
fragment DIGIT  : [0-9] ;

LEFT_BRACKET    : '(' ;
RIGHT_BRACKET   : ')' ;
STRING_LITERAL  : '\'' (~['])* '\'' ;
BOOLEAN_LITERAL : 'true' | 'false' ;
COMMA           : ',' ;

WS : [ \t\n\r] + -> skip ;
