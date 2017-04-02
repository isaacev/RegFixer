package edu.wisc.regfixer.fixer;

import java.util.List;
import edu.wisc.regfixer.parser.RegexNode;

public class Job {
  private final RegexNode originalRegex;
  private final List<Range> originalRanges;
  private final List<Range> selectedRanges;
  private final String corpus;
  private final Evaluator evaluator;

  public Job (RegexNode regex, List<Range> ranges, String corpus) {
    this.originalRegex = regex;
    this.selectedRanges = ranges;
    this.corpus = corpus;

    if (regex != null && ranges != null && corpus != null) {
      // The Evaluator instance is has methods for taking a synthesized regex and
      // testing it against the positive examples contained in `selectedRanges`
      // and the `corpus` to determine how good of a match the regex is.
      this.originalRanges = SearchEngine.getMatchingRanges(corpus, regex);
      this.evaluator = new Evaluator(this.originalRanges, this.selectedRanges, this.corpus);
    } else {
      this.originalRanges = null;
      this.evaluator = null;
    }
  }

  public RegexNode getOriginalRegex() {
    return this.originalRegex;
  }

  public List<Range> getOriginalRanges () {
    return this.originalRanges;
  }

  public List<Range> getSelectedRanges () {
    return this.selectedRanges;
  }

  public String getCorpus () {
    return this.corpus;
  }

  public Evaluator getEvaluator () {
    return this.evaluator;
  }
}
