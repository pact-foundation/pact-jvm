grammar MatcherDefinition;

@header {
    package au.com.dius.pact.core.model.matchingrules.expressions;

    import au.com.dius.pact.core.model.matchingrules.*;
    import au.com.dius.pact.core.model.generators.Generator;
    import au.com.dius.pact.core.support.Either;
    import java.util.Arrays;
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
      'matching' LEFT_BRACKET matchingRule RIGHT_BRACKET {
        if ($matchingRule.reference != null) {
          $value = new MatchingRuleDefinition($matchingRule.value, $matchingRule.reference, $matchingRule.generator);
        } else {
          $value = new MatchingRuleDefinition($matchingRule.value, $matchingRule.rule, $matchingRule.generator);
        }
      }
      | 'notEmpty' LEFT_BRACKET string RIGHT_BRACKET { $value = new MatchingRuleDefinition($string.contents, NotEmptyMatcher.INSTANCE, null); }
      | 'eachKey' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET { if ($e.value != null) { $value = new MatchingRuleDefinition(null, new EachKeyMatcher($e.value), null); } }
      | 'eachValue' LEFT_BRACKET e=matchingDefinitionExp RIGHT_BRACKET {
        if ($e.value != null) {
          $value = new MatchingRuleDefinition(null, ValueType.Unknown, List.of((Either<MatchingRule, MatchingReference>) new Either.A(new EachValueMatcher($e.value))), null);
        }
      }
    )
    ;

matchingRule returns [ String value, ValueType type, MatchingRule rule, Generator generator, MatchingReference reference ] :
  (
    ( 'equalTo' { $rule = EqualsMatcher.INSTANCE; }
    | 'type'  { $rule = TypeMatcher.INSTANCE; } )
    COMMA v=primitiveValue { $value = $v.value; $type = $v.type; } )
  | 'number' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.NUMBER); } COMMA val=( DECIMAL_LITERAL | INTEGER_LITERAL ) { $value = $val.getText(); $type = ValueType.Number; }
  | 'integer' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.INTEGER); } COMMA val=INTEGER_LITERAL { $value = $val.getText(); $type = ValueType.Integer; }
  | 'decimal' { $rule = new NumberTypeMatcher(NumberTypeMatcher.NumberType.DECIMAL); } COMMA val=DECIMAL_LITERAL { $value = $val.getText(); $type = ValueType.Decimal; }
  | matcherType=( 'datetime' | 'date' | 'time' ) COMMA format=string {
    if ($matcherType.getText().equals("datetime")) { $rule = new TimestampMatcher($format.contents); }
    if ($matcherType.getText().equals("date")) { $rule = new DateMatcher($format.contents); }
    if ($matcherType.getText().equals("time")) { $rule = new TimeMatcher($format.contents); }
    } COMMA s=string { $value = $s.contents; $type = ValueType.String; }
  | 'regex' COMMA r=string COMMA s=string { $rule = new RegexMatcher($r.contents); $value = $s.contents; $type = ValueType.String; }
  | 'include' COMMA s=string { $rule = new IncludeMatcher($s.contents); $value = $s.contents; $type = ValueType.String; }
  | 'boolean' COMMA BOOLEAN_LITERAL { $rule = BooleanMatcher.INSTANCE; $value = $BOOLEAN_LITERAL.getText(); $type = ValueType.Boolean; }
  | 'semver' COMMA s=string { $rule = SemverMatcher.INSTANCE; $value = $s.contents; $type = ValueType.String; }
  | 'contentType' COMMA ct=string COMMA s=string { $rule = new ContentTypeMatcher($ct.contents); $value = $s.contents; $type = ValueType.Unknown; }
  | DOLLAR ref=string { $reference = new MatchingReference($ref.contents); $type = ValueType.Unknown; }
  ;

primitiveValue returns [ String value, ValueType type ] :
  string { $value = $string.contents; $type = ValueType.String; }
  | v=DECIMAL_LITERAL { $value = $v.getText(); $type = ValueType.Decimal; }
  | v=INTEGER_LITERAL { $value = $v.getText(); $type = ValueType.Number; }
  | v=BOOLEAN_LITERAL { $value = $v.getText(); $type = ValueType.Boolean; }
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
DOLLAR          : '$';

WS : [ \t\n\r] + -> skip ;

