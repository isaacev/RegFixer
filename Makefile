cp = "libs/*:libs/deps:bin:src"

app:
	mkdir -p bin
	javac -d bin -cp $(cp) src/RegFixer/*.java

all: parser app
	echo "all built"

parser: lexer
	java -cp $(cp) java_cup.Main -expect 300 -destdir src/RegexParser src/RegexParser/Regex.cup

lexer:
	jflex -d src/RegexParser src/RegexParser/Regex.jlex

run:
	java -cp $(cp) RegFixer.Main "$(REGEX)"

clean: clean-parser clean-lexer
	rm -rf bin

clean-parser:
	rm src/RegexParser/parser.java
	rm src/RegexParser/sym.java

clean-lexer:
	rm src/RegexParser/Yylex.java

test: build
	java -cp $(cp) org.junit.runner.JUnitCore RegFixer.Test
