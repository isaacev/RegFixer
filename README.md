# RegFixer

## Install

Install the latest version of [Maven](https://maven.apache.org/).

Run `mvn install` to compile & bundle the project. The finished JAR will be available in `target/`.

## Running on terminal 

Running `java -jar target/regfixer.jar benchmarks/example1.txt` should produce the following report:

```
Given the regular expression:

  \w\w\w

That already matches the strings:

  ✗ (0:3)    abc
  ✗ (4:7)    def
  ✓ (8:11)   123
  ✓ (12:15)  456
  ✗ (16:19)  ghi

When it *should only* match the strings:

  ✓ (8:11)   123
  ✓ (12:15)  456

Start by identifying promising sub-expressions:

  ❑\w\w
  \w❑\w
  \w\w❑
  ❑\w
  \w❑

Then synthesize character class replacements:

  ✗ .\w\w
  ✗ \w\w\w
  ✓ \d\w\w

Results in the expression:

  \d\w\w

That matches the strings:

  ✓ (8:11)   123
  ✓ (12:15)  456

All done
```

## Running on local server

`java -jar target/regfixer.jar serve --port=8080`

local web application available at http://localhost:8080/

## Comment on each file and directory in edu.wisc.regfixer

- automata: builds an automata with a regular expression
- enumerate: builds a list of regular expressions with hole
- parser: builds a regular expression node with a given expression
- server: contains custom error classes
- synthesize: given a regular expression node with hole and both positive and negative examples, it builds SAT formula and solves it if possible class exists
- util: bunch of util classes
- CLi.java: contains custom terminal commands
- RegFixer.java: class that calls fixing methods
- Server.java: class related with the configuration of server

## Note
- java classes that Issac and Sang developed are in src/main/java/edu.wisc.regfixer, and rest directories/files are forked libraries
- the main class can be found in src/main/java/edu.wisc.regfixer/CLI
- nested call stacks can be track from there (‘handleFix’ method)
- It output more accurate result using Sang’s branch (major difference is SAT_Formula.java)
- Test result with the latest version can be found from attached excel file
- Z3 library is used for building & solving SAT formula (https://github.com/Z3Prover/z3)
