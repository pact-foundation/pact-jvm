grammar DateExpression;

@header {
    package au.com.dius.pact.core.support.generators.expressions;
}

expression returns [ DateBase dateBase = DateBase.NOW, List<Adjustment<DateOffsetType>> adj = new ArrayList<>() ] : ( base { $dateBase = $base.t; }
    | op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
    | base { $dateBase = $base.t; } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }
    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }  ( op duration {
        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
    } )*
    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); }
    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); } ( op duration {
        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
    } )*
    ) EOF
    ;

base returns [ DateBase t ] : 'now' { $t = DateBase.NOW; }
    | 'today' { $t = DateBase.TODAY; }
    | 'yesterday' { $t = DateBase.YESTERDAY; }
    | 'tomorrow' { $t = DateBase.TOMORROW; }
    ;

duration returns [ Adjustment<DateOffsetType> d ] : INT durationType { $d = new Adjustment<DateOffsetType>($durationType.type, $INT.int); } ;

durationType returns [ DateOffsetType type ] : 'day' { $type = DateOffsetType.DAY; }
    | DAYS { $type = DateOffsetType.DAY; }
    | 'week' { $type = DateOffsetType.WEEK; }
    | WEEKS { $type = DateOffsetType.WEEK; }
    | 'month' { $type = DateOffsetType.MONTH; }
    | MONTHS { $type = DateOffsetType.MONTH; }
    | 'year' { $type = DateOffsetType.YEAR; }
    | YEARS { $type = DateOffsetType.YEAR; }
    ;

op returns [ Operation o ] : '+' { $o = Operation.PLUS; }
    | '-' { $o = Operation.MINUS; }
    ;

offset returns [ DateOffsetType type, int val = 1 ] : 'day' { $type = DateOffsetType.DAY; }
    | 'week' { $type = DateOffsetType.WEEK; }
    | 'month' { $type = DateOffsetType.MONTH; }
    | 'year' { $type = DateOffsetType.YEAR; }
    | 'fortnight' { $type = DateOffsetType.WEEK; $val = 2; }
    | 'monday' { $type = DateOffsetType.MONDAY; }
    | 'mon' { $type = DateOffsetType.MONDAY; }
    | 'tuesday' { $type = DateOffsetType.TUESDAY; }
    | 'tues' { $type = DateOffsetType.TUESDAY; }
    | 'wednesday' { $type = DateOffsetType.WEDNESDAY; }
    | 'wed' { $type = DateOffsetType.WEDNESDAY; }
    | 'thursday' { $type = DateOffsetType.THURSDAY; }
    | 'thurs' { $type = DateOffsetType.THURSDAY; }
    | 'friday' { $type = DateOffsetType.FRIDAY; }
    | 'fri' { $type = DateOffsetType.FRIDAY; }
    | 'saturday' { $type = DateOffsetType.SATURDAY; }
    | 'sat' { $type = DateOffsetType.SATURDAY; }
    | 'sunday' { $type = DateOffsetType.SUNDAY; }
    | 'sun' { $type = DateOffsetType.SUNDAY; }
    | 'january' { $type = DateOffsetType.JAN; }
    | 'jan' { $type = DateOffsetType.JAN; }
    | 'february' { $type = DateOffsetType.FEB; }
    | 'feb' { $type = DateOffsetType.FEB; }
    | 'march' { $type = DateOffsetType.MAR; }
    | 'mar' { $type = DateOffsetType.MAR; }
    | 'april' { $type = DateOffsetType.APR; }
    | 'apr' { $type = DateOffsetType.APR; }
    | 'may' { $type = DateOffsetType.MAY; }
    | 'june' { $type = DateOffsetType.JUNE; }
    | 'jun' { $type = DateOffsetType.JUNE; }
    | 'july' { $type = DateOffsetType.JULY; }
    | 'jul' { $type = DateOffsetType.JULY; }
    | 'august' { $type = DateOffsetType.AUG; }
    | 'aug' { $type = DateOffsetType.AUG; }
    | 'september' { $type = DateOffsetType.SEP; }
    | 'sep' { $type = DateOffsetType.SEP; }
    | 'october' { $type = DateOffsetType.OCT; }
    | 'oct' { $type = DateOffsetType.OCT; }
    | 'november' { $type = DateOffsetType.NOV; }
    | 'nov' { $type = DateOffsetType.NOV; }
    | 'december' { $type = DateOffsetType.DEC; }
    | 'dec' { $type = DateOffsetType.DEC; }
    ;

INT : DIGIT+ ;
fragment DIGIT : [0-9] ;

WS : [ \t\n\r] + -> skip ;

DAYS : DAY 's'? ;
fragment DAY : 'day' ;
WEEKS : WEEK 's'? ;
fragment WEEK : 'week';
MONTHS : MONTH 's'? ;
fragment MONTH : 'month';
YEARS : YEAR 's'? ;
fragment YEAR : 'year';
