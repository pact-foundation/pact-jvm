grammar MatcherDefinition;

@header {
    package au.com.dius.pact.core.model.matchingrules.expressions;

    import au.com.dius.pact.core.model.matchingrules.*;
    import au.com.dius.pact.core.model.generators.Generator;
}

/**
* Parse a matcher definition into a tuple containing the example value, matching rule and any generator.
* The following are examples of matching rule definitions:
* * `matching(type,'Name')` - type matcher
* * `matching(number,100)` - number matcher
* * `matching(datetime, 'yyyy-MM-dd','2000-01-01')` - datetime matcher with format string
**/
matchingDefinition returns [ String value, MatchingRule rule, Generator generator ] :
    (
      'matching' LEFT_BRACKET matchingRule { $value = $matchingRule.value; $rule = $matchingRule.rule; $generator = $matchingRule.generator; } RIGHT_BRACKET
    ) EOF
    ;

matchingRule returns [ String value, MatchingRule rule, Generator generator ] :
  (
    ( 'equality' { $rule = EqualsMatcher.INSTANCE; }
    | 'type'  { $rule = TypeMatcher.INSTANCE; } )
    ',' string { $value = $string.contents; } )
  | 'number' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER); } ',' val=( DECIMAL_LITERAL | INTEGER_LITERAL ) { $value = $val.getText(); }
  | 'integer' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER); } ',' val=INTEGER_LITERAL { $value = $val.getText(); }
  | 'decimal' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL); } ',' val=DECIMAL_LITERAL { $value = $val.getText(); }
  | matcherType=( 'datetime' | 'date' | 'time' ) ',' format=string {
    if ($matcherType.getText().equals("datetime")) { $rule = new TimestampMatcher($format.contents); }
    if ($matcherType.getText().equals("date")) { $rule = new DateMatcher($format.contents); }
    if ($matcherType.getText().equals("time")) { $rule = new TimeMatcher($format.contents); }
    } ',' v=string { $value = $v.contents; }
  | 'regex' ',' r=string ',' v=string { $rule = new RegexMatcher($r.contents); $value = $v.contents; }
  | 'include' ',' v=string { $rule = new IncludeMatcher($v.contents); $value = $v.contents; }
  | 'boolean' ',' BOOLEAN_LITERAL { $rule = BooleanMatcher.INSTANCE; $value = $BOOLEAN_LITERAL.getText(); }
  ;

string returns [ String contents ] :
  STRING_LITERAL {
    String contents = $STRING_LITERAL.getText();
    $contents = contents.substring(1, contents.length() - 1);
  }
  ;

INTEGER_LITERAL : '-'? DIGIT+ ;
DECIMAL_LITERAL : '-'? DIGIT+ '.' DIGIT+ ;
fragment DIGIT  : [0-9] ;

LEFT_BRACKET    : '(' ;
RIGHT_BRACKET   : ')' ;
STRING_LITERAL  : '\'' (~['])* '\'' ;
BOOLEAN_LITERAL : 'true' | 'false' ;

WS : [ \t\n\r] + -> skip ;
