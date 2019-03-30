grammar DateExpression;

@header {
    package au.com.dius.pact.core.support.generators.expressions;
}

expression returns [ DateBase dateBase = DateBase.NOW, List<Adjustment> adj = new ArrayList<>() ] : ( base { $dateBase = $base.t; }
    | op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
    | base { $dateBase = $base.t; } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }
    | 'next' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.PLUS)); }  op duration {
        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
    }
    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); }
    | 'last' offset { $adj.add(new Adjustment($offset.type, $offset.val, Operation.MINUS)); } (op duration {
        if ($duration.d != null) $adj.add($duration.d.withOperation($op.o));
    })*
    ) EOF
    ;

base returns [ DateBase t ] : 'now' { $t = DateBase.NOW; }
    | 'today' { $t = DateBase.TODAY; }
    | 'yesterday' { $t = DateBase.YESTERDAY; }
    | 'tomorrow' { $t = DateBase.TOMORROW; }
    ;

duration returns [ Adjustment d ] : INT durationType { $d = new Adjustment($durationType.type, $INT.int); } ;

durationType returns [ OffsetType type ] : DAYS { $type = OffsetType.DAY; }
    | WEEKS { $type = OffsetType.WEEK; }
    | MONTHS { $type = OffsetType.MONTH; }
    | YEARS { $type = OffsetType.YEAR; }
    ;

op returns [ Operation o ] : '+' { $o = Operation.PLUS; }
    | '-' { $o = Operation.MINUS; }
    ;

offset returns [ OffsetType type, int val = 1 ] : 'day' { $type = OffsetType.DAY; }
    | 'week' { $type = OffsetType.WEEK; }
    | 'month' { $type = OffsetType.MONTH; }
    | 'year' { $type = OffsetType.YEAR; }
    | 'fortnight' { $type = OffsetType.WEEK; $val = 2; }
    | 'monday' { $type = OffsetType.MONDAY; }
    | 'mon' { $type = OffsetType.MONDAY; }
    | 'tuesday' { $type = OffsetType.TUESDAY; }
    | 'tues' { $type = OffsetType.TUESDAY; }
    | 'wednesday' { $type = OffsetType.WEDNESDAY; }
    | 'wed' { $type = OffsetType.WEDNESDAY; }
    | 'thursday' { $type = OffsetType.THURSDAY; }
    | 'thurs' { $type = OffsetType.THURSDAY; }
    | 'friday' { $type = OffsetType.FRIDAY; }
    | 'fri' { $type = OffsetType.FRIDAY; }
    | 'saturday' { $type = OffsetType.SATURDAY; }
    | 'sat' { $type = OffsetType.SATURDAY; }
    | 'sunday' { $type = OffsetType.SUNDAY; }
    | 'sun' { $type = OffsetType.SUNDAY; }
    | 'january' { $type = OffsetType.JAN; }
    | 'jan' { $type = OffsetType.JAN; }
    | 'febuary' { $type = OffsetType.FEB; }
    | 'feb' { $type = OffsetType.FEB; }
    | 'march' { $type = OffsetType.MAR; }
    | 'mar' { $type = OffsetType.MAR; }
    | 'april' { $type = OffsetType.APR; }
    | 'apr' { $type = OffsetType.APR; }
    | 'may' { $type = OffsetType.MAY; }
    | 'june' { $type = OffsetType.JUNE; }
    | 'jun' { $type = OffsetType.JUNE; }
    | 'july' { $type = OffsetType.JULY; }
    | 'jul' { $type = OffsetType.JULY; }
    | 'august' { $type = OffsetType.AUG; }
    | 'aug' { $type = OffsetType.AUG; }
    | 'september' { $type = OffsetType.SEP; }
    | 'sep' { $type = OffsetType.SEP; }
    | 'october' { $type = OffsetType.OCT; }
    | 'oct' { $type = OffsetType.OCT; }
    | 'november' { $type = OffsetType.NOV; }
    | 'nov' { $type = OffsetType.NOV; }
    | 'december' { $type = OffsetType.DEC; }
    | 'dec' { $type = OffsetType.DEC; }
    ;

INT : DIGIT+ ;
fragment DIGIT : [0-9] ;

WS : [ \t\n\r] + -> skip ;

DAYS : DAY 's'? ;
DAY : 'day';
WEEKS : 'week' 's'? ;
MONTHS : 'month' 's'? ;
YEARS : 'year' 's'? ;
