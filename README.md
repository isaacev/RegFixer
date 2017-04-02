# RegFixer

Install the latest version of [Maven](https://maven.apache.org/).

Run `mvn install` to compile & bundle the project. The finished JAR will be available in `target/`.

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
