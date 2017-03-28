package RegFixer;

import java.util.*;
import java.util.regex.*;
import RegexParser.*;

class Evaluator {
  private String corpus;

  Evaluator (String corpus) {
    this.corpus = corpus;
  }

  List<Range> test (RegexNode regex) {
    Matcher mt = Pattern.compile(regex.toString()).matcher(this.corpus);
    List<Range> matches = new LinkedList<Range>();

    while (mt.find()) {
      Range match = new Range(mt.start(), mt.end());
      matches.add(match);
    }

    return matches;
  }
}
