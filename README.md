# RegFixer

Install the latest version of [Maven](https://maven.apache.org/).

Run `mvn install` to compile & bundle the project. The finished JAR will be available in `target/`.

Running `java -jar target/regfixer.jar benchmarks/example1.txt` should produce the following report:

<pre><code>
Given the regular expression:

  <span style="color:blue">\w\w\w</span>

That already matches the strings:

  <span style="color:red">✗ (0:3)    abc</span>
  <span style="color:red">✗ (4:7)    def</span>
  <span style="color:green">✓ (8:11)   123</span>
  <span style="color:green">✓ (12:15)  456</span>
  <span style="color:red">✗ (16:19)  ghi</span>

When it *should only* match the strings:

  <span style="color:green">✓ (8:11)   123</span>
  <span style="color:green">✓ (12:15)  456</span>

Start by identifying promising sub-expressions:

  <span style="color:blue">❑\w\w</span>
  <span style="color:blue">\w❑\w</span>
  <span style="color:blue">\w\w❑</span>
  <span style="color:blue">❑\w</span>
  <span style="color:blue">\w❑</span>

Then synthesize character class replacements:

  <span style="color:red">✗ .\w\w</span>
  <span style="color:red">✗ \w\w\w</span>
  <span style="color:green">✓ \d\w\w</span>

Results in the expression:

  <span style="color:blue">\d\w\w</span>

That matches the strings:

  <span style="color:green">✓ (8:11)   123</span>
  <span style="color:green">✓ (12:15)  456</span>

All done
</code></pre>
