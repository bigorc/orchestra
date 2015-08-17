/**
 * Define a grammar called Hello
 */
grammar Sentence;

sentence
  : WORD+ BE WORD
  ;

BE: WS 'is'|'are' ; 
STRING
 : '"' (~[\r\n"] | '""')+ '"'
 ;
CHAR: ~(' '|'\t'|'\n'|'\r') ;
WORD: CHAR+ ;
NL:'\r'?'\n' ;
WS: [ \t]+ -> skip;
