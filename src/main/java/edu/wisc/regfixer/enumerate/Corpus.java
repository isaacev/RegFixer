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
      .map(r -> this.corpus.substring(r.getLeftIndex(), r.getRightIndex()))
      .collect(Collectors.toSet());

    this.negativeExamples = this.negativeRanges.stream()
      .map(r -> this.corpus.substring(r.getLeftIndex(), r.getRightIndex()))
      .collect(Collectors.toSet());
  }

  public String getCorpus () {
    return this.corpus;
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
    List<Range> oldRanges = new LinkedList<>(getMatchingRanges(pattern, corpus));
    List<Range> newRanges = new LinkedList<>(positives);
    Collections.sort(oldRanges);
    Collections.sort(newRanges);

    Set<Range> negatives = new HashSet<>();
    int oldIndex = 0;
    int newIndex = 0;

    outerLoop:
    while (true) {
      boolean exhaustedOldRanges = (oldIndex >= oldRanges.size());
      boolean exhaustedNewRanges = (newIndex >= newRanges.size());
      if (exhaustedOldRanges || exhaustedNewRanges) {
        break outerLoop;
      }

      Range oldRange = oldRanges.get(oldIndex++);
      Range newRange = newRanges.get(newIndex++);

      // Ignore "old" ranges that don't intersect the current "new" ranges and
      // that comre before the current "new" range.
      while (oldRange.endsBefore(newRange)) {
        if (oldIndex >= oldRanges.size()) {
          break outerLoop;
        }
      }

      if (oldRange.startsBefore(newRange) && oldRange.intersects(newRange)) {
        int from = newRange.getLeftIndex();
        int to = oldRange.getRightIndex();
        negatives.add(new Range(from, to));
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
