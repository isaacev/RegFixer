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

  static List<Range> derriveNegativeExamples (List<Range> oldExamples, List<Range> newExamples) {
    List<Range> negExamples = new LinkedList<Range>();

    Collections.sort(oldExamples);
    Collections.sort(newExamples);

    outerLoop:
    for (int i_old = 0, i_new = 0; i_new < newExamples.size() && i_old < oldExamples.size(); i_new++) {
      Range oldEx = oldExamples.get(i_old++);
      Range newEx = newExamples.get(i_new);

      // Ignore old ranges that don't intersect the current new range and come
      // before the current new range.
      while (oldEx.endsBefore(newEx)) {
        if (i_old >= oldExamples.size()) {
          break outerLoop;
        }

        oldEx = oldExamples.get(i_old++);
      }

      if (oldEx.startsBefore(newEx) && oldEx.intersects(newEx)) {
        Range negEx = new Range(newEx.getLeftIndex(), oldEx.getRightIndex());
        negExamples.add(negEx);
      }
    }

    return negExamples;
  }
}
