package edu.wisc.regfixer.enumerate;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

  public Corpus (String corpus, Set<Range> positives, Set<Range> negatives) {
    this.corpus = corpus;
    this.positiveRanges = new HashSet<Range>(positives);
    this.negativeRanges = new HashSet<Range>(negatives);

    this.positiveExamples = this.positiveRanges.stream()
      .map(r -> this.getSubstring(r))
      .collect(Collectors.toSet());

    this.negativeExamples = this.negativeRanges.stream()
      .map(r -> this.getSubstring(r))
      .collect(Collectors.toSet());
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
    return (false == matchesStrings(pattern, this.negativeExamples));
  }

  public void addNegativeMatches (Set<Range> newNegatives) {
    this.negativeRanges.addAll(newNegatives);
  }

  public boolean noUnexpectedMatches (Synthesis synthesis) {
    Set<Range> ranges = getMatchingRanges(synthesis.toPattern(), this.corpus);
    return this.positiveRanges.containsAll(ranges);
  }

  public Set<Range> findUnexpectedMatches (Synthesis synthesis) {
    // TODO
    return null;
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
    Set<Range> negatives = new HashSet<>();

    Collections.sort(found);
    Collections.sort(expected);

    int oldIndex = 0;
    int newIndex = 0;

    while (true) {
      if (oldIndex >= found.size()) {
        break;
      }

      if (newIndex >= expected.size()) {
        negatives.add(found.get(oldIndex++));
        continue;
      }

      Range oldRange = found.get(oldIndex);
      Range newRange = expected.get(newIndex);

      if (newRange.getLeftIndex() == oldRange.getLeftIndex()) {
        if (newRange.equals(oldRange) == false) {
          negatives.add(oldRange);
        }

        if (oldRange.length() <= newRange.length()) {
          oldIndex++;
        }

        if (newRange.length() <= oldRange.length()) {
          newIndex++;
        }
      } else if (newRange.getLeftIndex() < oldRange.getLeftIndex()) {
        if (newRange.endsBefore(oldRange) || newRange.intersects(oldRange)) {
          newIndex++;
        } else {
          oldIndex++;
        }
      } else if (newRange.getLeftIndex() > oldRange.getLeftIndex()) {
        negatives.add(oldRange);

        if (oldRange.endsBefore(newRange) || oldRange.intersects(newRange)) {
          oldIndex++;
        } else {
          newIndex++;
        }
      }
    }

    return negatives;
  }

  private static Set<Range> getMatchingRanges (Pattern pattern, String corpus) {
    Set<Range> ranges = new HashSet<>();
    Matcher matcher = pattern.matcher(corpus);

    while (matcher.find()) {
      ranges.add(new Range(matcher.start(), matcher.end()));
    }

    return ranges;
  }
}
