# RegFixer

Run `make all` to build the lexer, parser, and RegFixer libraries.

Running `java -cp "bin:libs/*" RegFixer.Main "abc"` should produce the results:

```
🐱bc
a🐱c
ab🐱
🐱c
a🐱
🐱
```

Where each 🐱 (cat emoji) represents an expression hole generated in the given regular expression.
