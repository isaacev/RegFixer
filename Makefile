build_cp = "libs/*:libs/deps:src"
run_cp   = "libs/*:bin"

app:
	mkdir -p bin
	javac -d bin -cp $(build_cp) src/RegFixer/*.java

all: parser app
	echo "all built"

parser: clean-parser lexer
	java -cp $(build_cp) java_cup.Main -expect 300 -destdir src/RegexParser src/RegexParser/Regex.cup

lexer: clean-lexer
	jflex -d src/RegexParser src/RegexParser/Regex.jlex

run:
	java -cp $(run_cp) RegFixer.Main

clean-tests:
	rm -rf bin/RegFixer/tests

build-tests:
	javac -cp $(build_cp) src/RegFixer/tests/*.java -d bin

test: clean-tests build-tests
	java -cp $(run_cp) RegFixer.TestRunner

clean: clean-parser clean-lexer
	rm -rf bin

clean-parser:
	rm -f src/RegexParser/parser.java
	rm -f src/RegexParser/sym.java

clean-lexer:
	rm -f src/RegexParser/Yylex.java
