package edu.wisc.regfixer.parser;

import java_cup.runtime.*;

class TokenVal {
  // fields
    int linenum;
    int charnum;
  // constructor
    TokenVal(int line, int ch) {
        linenum = line;
        charnum = ch;
    }
}


class CharTokenVal extends TokenVal {
  // new field: the value of the identifier
    char charVal;
  // constructor
    CharTokenVal(int line, int ch, char val) {
        super(line, ch);
    charVal = val;
    }
}



// the current token starts on its line.
class CharNum {
    static int num=1;
}
%%

DIGIT=     [0-9]
NOTDIGIT = [^0-9]
ESCAPEDCHAR = [\\][^tnrfbBdDsSvwW]



%implements java_cup.runtime.Scanner
%function next_token
%type java_cup.runtime.Symbol

%eofval{
CharNum.num =1;
return new Symbol(sym.EOF);
%eofval}

%line

%%

\n        {
            CharNum.num = 1;
          }

"\\t"      { Symbol S = new Symbol(sym.METATAB, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\n"      { Symbol S = new Symbol(sym.METANEWLINE, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\r"      { Symbol S = new Symbol(sym.METARETURN, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\f"      { Symbol S = new Symbol(sym.METAFEED, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\b"      { Symbol S = new Symbol(sym.METABOUNDARY, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\B"      { Symbol S = new Symbol(sym.METANOTBOUNDARY, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\d"      { Symbol S = new Symbol(sym.METADIGIT, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\D"      { Symbol S = new Symbol(sym.METANOTDIGIT, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\s"      { Symbol S = new Symbol(sym.METASPACE, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\S"      { Symbol S = new Symbol(sym.METANOTSPACE, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\v"      { Symbol S = new Symbol(sym.METAVERTICALTAB, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\w"      { Symbol S = new Symbol(sym.METAWORD, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }

"\\W"      { Symbol S = new Symbol(sym.METANOTWORD, new TokenVal(yyline+1, CharNum.num));
            CharNum.num+=2;
            return S;
          }


{ESCAPEDCHAR} {
            String val = yytext();
            char charVal = val.charAt(1);
            Symbol S = new Symbol(sym.ESCAPEDCHAR,
                             new CharTokenVal(yyline+1, CharNum.num, charVal));
            CharNum.num += yytext().length();
            return S;
          }

"-"       { Symbol S = new Symbol(sym.MINUS, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"."       { Symbol S = new Symbol(sym.DOT, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }



"+"       { Symbol S = new Symbol(sym.PLUS, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"*"       { Symbol S = new Symbol(sym.STAR, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"?"       { Symbol S = new Symbol(sym.OPTIONAL, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"^"       { Symbol S = new Symbol(sym.CARET, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }
"$"       { Symbol S = new Symbol(sym.DOLLAR, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"["       { Symbol S = new Symbol(sym.LBRACKET, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"]"       { Symbol S = new Symbol(sym.RBRACKET, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"("       { Symbol S = new Symbol(sym.LPAREN, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

")"       { Symbol S = new Symbol(sym.RPAREN, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"{"       { Symbol S = new Symbol(sym.LCURLY, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"}"       { Symbol S = new Symbol(sym.RCURLY, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

"|"       { Symbol S = new Symbol(sym.UNION, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }

","       { Symbol S = new Symbol(sym.COMMA, new TokenVal(yyline+1, CharNum.num));
            CharNum.num++;
            return S;
          }




{DIGIT}   {
            String val = yytext();
            char charVal = val.charAt(0);
            Symbol S = new Symbol(sym.DIGIT,
                             new CharTokenVal(yyline+1, CharNum.num, charVal));
            CharNum.num += yytext().length();
            return S;
          }

{NOTDIGIT} {
            String val = yytext();
            char charVal = val.charAt(0);
            Symbol S = new Symbol(sym.NOTDIGIT,
                             new CharTokenVal(yyline+1, CharNum.num, charVal));
            CharNum.num += yytext().length();
            return S;
          }

