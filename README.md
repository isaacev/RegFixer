# RegFixer

On macOS with homebrew run `brew install jflex` to install Jflex.

Run `make all` to build the lexer, parser, and RegFixer libraries.

Running `make run` should produce the following results:

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
