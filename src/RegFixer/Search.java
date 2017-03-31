package RegFixer;

import java.util.*;
import java.util.regex.*;
import RegexParser.*;

public class Search {
  public static List<Range> getMatchingRanges (String corpus, RegexNode regex) {
    Matcher mt = Pattern.compile(regex.toString()).matcher(corpus);
    List<Range> ranges = new LinkedList<Range>();

    while (mt.find()) {
      Range range = new Range(mt.start(), mt.end());
      ranges.add(range);
    }

    return ranges;
  }

  public static List<String> getMatchingStrings (String corpus, RegexNode regex) {
    List<Range> ranges = getMatchingRanges(corpus, regex);
    List<String> substrings = getMatchingStrings(corpus, ranges);

    return substrings;
  }

  public static List<String> getMatchingStrings (String corpus, List<Range> ranges) {
    List<String> substrings = new LinkedList<String>();

    for (Range range : ranges) {
      substrings.add(getMatchingString(corpus, range));
    }

    return substrings;
  }

  public static String getMatchingString (String corpus, Range range) {
    int from = range.getLeftIndex();
    int to = range.getRightIndex();

    return corpus.substring(from, to);
  }

  public static List<Range> inferNegativeRanges (List<Range> oldRanges, List<Range> newRanges) {
    Collections.sort(oldRanges);
    Collections.sort(newRanges);

    List<Range> negRanges = new LinkedList<Range>();
    int oldIndex = 0;
    int newIndex = 0;

    whileLoop:
    while (true) {
      boolean exhaustedOldRanges = (oldIndex >= oldRanges.size());
      boolean exhaustedNewRanges = (newIndex >= newRanges.size());
      if (exhaustedOldRanges || exhaustedNewRanges) {
        break whileLoop;
      }

      Range oldRange = oldRanges.get(oldIndex++);
      Range newRange = newRanges.get(newIndex++);

      // Ignore "old" ranges that don't intersect the current "new" match and come
      // before the current "new" match.
      while (oldRange.endsBefore(newRange)) {
        if (oldIndex >= oldRanges.size()) {
          break whileLoop;
        }

        oldRange = oldRanges.get(oldIndex++);
      }

      if (oldRange.startsBefore(newRange) && oldRange.intersects(newRange)) {
        int from = newRange.getLeftIndex();
        int to = oldRange.getRightIndex();
        Range negRange = new Range(from, to);
        negRanges.add(negRange);
      }
    }

    return negRanges;
  }
}
