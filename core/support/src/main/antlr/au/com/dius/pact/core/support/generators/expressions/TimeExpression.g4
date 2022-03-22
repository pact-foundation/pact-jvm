grammar TimeExpression;

@header {
    package au.com.dius.pact.core.support.generators.expressions;
}

expression returns [ TimeBase timeBase = TimeBase.Now.INSTANCE, List<Adjustment<TimeOffsetType>> adj = new ArrayList<>() ] : ( base { $timeBase = $base.t; }
    | op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
    | base { $timeBase = $base.t; } ( op duration { if ($duration.d != null) $adj.add($duration.d.withOperation($op.o)); } )*
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

base returns [ TimeBase t ] : 'now' { $t = TimeBase.Now.INSTANCE; }
    | 'midnight' { $t = TimeBase.Midnight.INSTANCE; }
    | 'noon' { $t = TimeBase.Noon.INSTANCE; }
    | INT oclock { $t = TimeBase.of($INT.int, $oclock.h); }
    ;

oclock returns [ ClockHour h ] : 'o\'clock' 'am' { $h = ClockHour.AM; }
    | 'o\'clock' 'pm' { $h = ClockHour.PM; }
    | 'o\'clock' { $h = ClockHour.NEXT; }
    ;

duration returns [ Adjustment<TimeOffsetType> d ] : INT durationType { $d = new Adjustment<TimeOffsetType>($durationType.type, $INT.int); } ;

durationType returns [ TimeOffsetType type ] : 'hour' { $type = TimeOffsetType.HOUR; }
    | HOURS { $type = TimeOffsetType.HOUR; }
    | 'minute' { $type = TimeOffsetType.MINUTE; }
    | MINUTES { $type = TimeOffsetType.MINUTE; }
    | 'second' { $type = TimeOffsetType.SECOND; }
    | SECONDS { $type = TimeOffsetType.SECOND; }
    | 'millisecond' { $type = TimeOffsetType.MILLISECOND; }
    | MILLISECONDS { $type = TimeOffsetType.MILLISECOND; }
    ;

op returns [ Operation o ] : '+' { $o = Operation.PLUS; }
    | '-' { $o = Operation.MINUS; }
    ;

offset returns [ TimeOffsetType type, int val = 1 ] : 'hour' { $type = TimeOffsetType.HOUR; }
    | 'minute' { $type = TimeOffsetType.MINUTE; }
    | 'second' { $type = TimeOffsetType.SECOND; }
    | 'millisecond' { $type = TimeOffsetType.MILLISECOND; }
    ;

INT : DIGIT+ ;
fragment DIGIT : [0-9] ;

WS : [ \t\n\r] + -> skip ;

HOURS : 'hour' 's'? ;
SECONDS : 'second' 's'? ;
MINUTES : 'minute' 's'? ;
MILLISECONDS : 'millisecond' 's'? ;
