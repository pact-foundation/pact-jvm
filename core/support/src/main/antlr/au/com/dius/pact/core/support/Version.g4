grammar Version;

@header {
    package au.com.dius.pact.core.support;
}

version returns [ Version v ] :
  { Integer major, minor, patch = null; }
  INT { major = $INT.int; } '.' INT { minor = $INT.int; } ('.' INT { patch = $INT.int; })? EOF {
    if (patch != null) {
      $v = new Version(major, minor, patch);
    } else {
      $v = new Version(major, minor);
    }
  }
  ;

INT : DIGIT+ ;
fragment DIGIT : [0-9] ;
