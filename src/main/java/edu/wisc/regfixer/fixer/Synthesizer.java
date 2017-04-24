package edu.wisc.regfixer.fixer;

import java.util.List;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.util.ReportStream;

public class Synthesizer {
  public static RegexNode synthesize (Job job, List<IncompleteTree> trees) {
    for (IncompleteTree tree : trees) {
      RegexNode result = synthesizeTree(job, tree);

      if (result != null) {
        return result;
      }
    }

    return null;
  }

  public static RegexNode synthesize (Job job, List<IncompleteTree> trees, ReportStream report) {
    report.printHeader("Then synthesize character class replacements:");
    for (IncompleteTree tree : trees) {
      RegexNode result = synthesizeTree(job, tree);
      report.printRegexStatus((result != null), tree);

      if (result != null) {
        return result;
      }
    }

    return null;
  }

  public static RegexNode synthesizeTree (Job job, IncompleteTree tree) {
    // 1. assume all holes can be filled with a character class
    // 2. replace all holes with a character class (start with dot then refine)
    // 3. if class DOES NOT match all P, the hole must be more complex than
    //    a single character class, so goto SPLIT
    // 4. if class matches all P and rejects all N, RETURN regex tree
    // 5. choose more restrictive class, then goto #2
    //
    // SPLIT: choose a compound node { Concat, Star, Plus, Union } and use the
    //    compound node to add 1 or more progeny holes and repeat from #2 for
    //    each of the new holes

    CharClassAdviser suggest = new CharClassAdviser();
    RegexNode twig = null;

    while (true) {
      if ((twig = suggest.nextSuggestion()) == null) {
        break;
      }

      tree.completeWith(twig);

      if (job.getEvaluator().passesPositiveExampleTest(tree)) {
        // Regex created from `tree` matches all positive examples.
        if (job.getEvaluator().passesNegativeExampleTest(tree)) {
          // Regex created from `tree` rejects all nevative examples.
          return tree;
        }
      } else {
        // Replace hole in `tree` with some more complex regex node since the
        // positive examples can't all be satisfied by replacing the hole with
        // a single character class.
        System.err.printf("%s needs to be expanded\n", tree);
        System.exit(1);
      }
    }

    return null;
  }
}
