package edu.wisc.regfixer.fixer;

import java.util.List;
import edu.wisc.regfixer.util.ReportStream;

public class Analyzer {
  public static List<PartialTree> analyze (Job job) {
    // Get a list of unique trees where each tree has 1 sub-expression replaced
    // by a hole that can be filled in the future by a synthesized expression.
    List<PartialTree> forest = Termite.digest(job.getOriginalRegex());

    // The `pruneForest` method removes any incomplete trees (trees with at
    // least 1 hole) for which adding `.*` does NOT accept all positive examples
    // and for which adding `empty set` does NOT reject all negative examples.
    List<PartialTree> pruned = job.getEvaluator().pruneForest(forest);

    return pruned;
  }

  public static List<PartialTree> analyze (Job job, ReportStream report) {
    report.printHeader("Given the regular expression:");
    report.printRegex(job.getOriginalRegex());

    report.printHeader("That already matches the strings:");
    for (int i = 0; i < job.getEvaluator().getOriginalRanges().size(); i++) {
      String match = job.getEvaluator().getOriginalStrings().get(i);
      Range  range = job.getEvaluator().getOriginalRanges().get(i);
      boolean isOK = job.getEvaluator().getSelectedStrings().contains(match);
      report.printMatchStatus(isOK, range, match);
    }

    report.printHeader("When it should ONLY match the strings:");
    for (int i = 0; i < job.getEvaluator().getSelectedRanges().size(); i++) {
      String match = job.getEvaluator().getSelectedStrings().get(i);
      Range  range = job.getEvaluator().getSelectedRanges().get(i);
      boolean isOK = job.getEvaluator().getOriginalStrings().contains(match);
      report.printMatchStatus(isOK, range, match);
    }

    List<PartialTree> pruned = analyze(job);

    report.printHeader("Start by identifying promising sub-expressions:");
    for (PartialTree tree : pruned) {
      report.printRegex(tree);
    }

    return pruned;
  }
}
