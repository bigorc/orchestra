grammar Rules;

prog: line+ EOF;

line: cons NEWLINE
	| before_cons NEWLINE
	| NEWLINE
	;


on_host: ON WORD ;
sm_cons: WORD OF? WORD+ BE LIKE? WORD+ ;
sm_host_cons: WORD OF? WORD+ BE LIKE? WORD+ on_host;
rjo_cons: STRING ;
rjo_host_cons: STRING on_host;
cons: sm_cons | sm_host_cons | rjo_cons | rjo_host_cons ;
before_cons: cons BEFORE cons ;

STRING
 : '"' (~[\r\n"] | '""')+ '"'
 ;

OF: 'of';
LIKE: 'like';
BE: 'is'|'are' ;
ON: 'on' ;
BEFORE: 'before';
WORD: CHAR+ ;
WS: [ \t]+ -> skip;
CHAR: ~(' '|'\t'|'\n'|'\r') ;
NEWLINE:'\r'? '\n' ; // return newlines to parser (is end-statement signal)
EL: NEWLINE|EOF ;

