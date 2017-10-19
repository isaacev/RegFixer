package edu.wisc.regfixer.enumerate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.wisc.regfixer.synthesize.Synthesis;

public class Corpus {
  private final String corpus;
  private final Set<Range> positiveRanges;
  private final Set<Range> negativeRanges;
  private final Set<String> positiveExamples;
  private final Set<String> negativeExamples;

  // private Set<Range> interestingRanges;

  public Corpus (String corpus, Set<Range> positives, Set<Range> negatives) {
    this.corpus = corpus;
    this.positiveRanges = new TreeSet<Range>(positives);
    this.negativeRanges = new TreeSet<Range>(negatives);

    this.positiveExamples = this.positiveRanges.stream()
      .map(r -> this.getSubstring(r))
      .collect(Collectors.toCollection(TreeSet::new));

    this.negativeExamples = this.negativeRanges.stream()
      .map(r -> this.getSubstring(r))
      .collect(Collectors.toCollection(TreeSet::new));
  }

  public String getCorpus () {
    return this.corpus;
  }

  public String getSubstring (Range range) {
    return this.corpus.substring(range.getLeftIndex(), range.getRightIndex());
  }

  public Set<Range> getPositiveRanges () {
    return this.positiveRanges;
  }

  public Set<Range> getNegativeRanges () {
    return this.negativeRanges;
  }

  public Set<String> getPositiveExamples () {
    return this.positiveExamples;
  }

  public Set<String> getNegativeExamples () {
    return this.negativeExamples;
  }

  public boolean passesDotTest (Enumerant enumerant) {
    Pattern pattern = enumerant.toPattern(HoleNode.FillType.Dot);
    return matchesStrings(pattern, this.positiveExamples);
  }

  public boolean passesDotStarTest (Enumerant enumerant) {
    Pattern pattern = enumerant.toPattern(HoleNode.FillType.DotStar);
    return matchesStrings(pattern, this.positiveExamples);
  }

  public boolean passesEmptySetTest (Enumerant enumerant) {
    Pattern pattern = enumerant.toPattern(HoleNode.FillType.EmptySet);
    Set<Range> ranges = getMatchingRanges(pattern, this.corpus);

    ranges.removeAll(this.positiveRanges);

    outer:
    for (Range r : ranges) {
      for (Range p : this.positiveRanges) {
        if (p.startsBefore(r) && p.startsAfter(r)) {
          continue outer;
        }
      }

      // interestingRanges.add(r);
      ranges.remove(r);
    }

    if (ranges.size() > 0) {
      return false;
    } else {
      return true;
    }
  }

  public void addNegativeMatches (Set<Range> newNegatives) {
    this.negativeRanges.addAll(newNegatives);
  }

  public boolean noUnexpectedMatches (Synthesis synthesis) {
    Set<Range> ranges = getMatchingRanges(synthesis.toPattern(), this.corpus);
    return this.positiveRanges.containsAll(ranges);
  }

  public boolean isPerfectMatch (Synthesis synthesis) {
    Set<Range> ranges = getMatchingRanges(synthesis.toPattern(), this.corpus);
    return ranges.equals(this.positiveRanges);
  }

  public Set<Range> getBadMatches (Synthesis synthesis) {
    Set<Range> ranges = getMatchingRanges(synthesis.toPattern(), this.corpus);
    ranges.removeAll(this.positiveRanges);
    return ranges;
  }

  public Set<Range> findUnexpectedMatches (Synthesis synthesis) {
    Set<Range> found = getMatchingRanges(synthesis.toPattern(), this.corpus);
    return Corpus.inferNegativeRanges(found, this.positiveRanges);
  }

  private boolean matchesStrings (Pattern pattern, Set<String> strings) {
    for (String s : strings) {
      if (false == pattern.matcher(s).matches()) {
        return false;
      }
    }

    return true;
  }

  public static Set<Range> inferNegativeRanges (Pattern pattern, String corpus, Set<Range> positives) {
    Set<Range> found = getMatchingRanges(pattern, corpus);
    return inferNegativeRanges(found, positives);
  }

  public static Set<Range> inferNegativeRanges (Set<Range> found, Set<Range> expected) {
    List<Range> foundList = new LinkedList<>(found);
    List<Range> expectedList = new LinkedList<>(expected);
    return inferNegativeRanges(foundList, expectedList);
  }

  public static Set<Range> inferNegativeRanges (List<Range> found, List<Range> expected) {
    Set<Range> negatives = new TreeSet<>();

    Collections.sort(found);
    Collections.sort(expected);

    for (Range maybe : found) {
      if (isNegativeRange(maybe, expected)) {
        negatives.add(maybe);
      }
    }

    return negatives;
  }

  public static boolean isNegativeRange (Range maybe, List<Range> positives) {
    for (int i = 0; i < positives.size(); i++) {
      Range pos = positives.get(i);

      if (pos.equals(maybe)) {
        return false;
      }

      if (pos.getLeftIndex() == maybe.getLeftIndex()) {
        if (maybe.endsAfter(pos)) {
          return true;
        }

        return false;
      }

      if (maybe.intersects(pos)) {
        return false;
      }

      if (pos.endsBefore(maybe)) {
        if (i == positives.size() - 1) {
          return true;
        } else if (positives.get(i + 1).startsAfter(maybe)) {
          return true;
        }
      }
    }

    return false;
  }

  private static Set<Range> getMatchingRanges (Pattern pattern, String corpus) {
    Set<Range> ranges = new TreeSet<>();
    Matcher matcher = pattern.matcher(corpus);

    while (matcher.find()) {
      ranges.add(new Range(matcher.start(), matcher.end()));
    }

    return ranges;
  }
}
