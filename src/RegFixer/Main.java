package regfixer;

import java.util.*;
import java.util.stream.*;
import RegexParser.*;
import static RegFixer.CorpusSearchEngine.*;

public class Main {
  public static void main (String[] args) throws Exception {
    if (args[0].equals("--no-color")) {
      Ansi.disableColor();
    }

    if (args.length == 0) {
      System.out.println("Usage: java RegFixer [options] [benchmark filepath]");
    }

    Benchmark bm = Benchmark.readFromFile(args[args.length - 1]);
    Evaluator ev = new Evaluator(
      bm.getCorpus(),
      bm.getOriginalRanges(),
      bm.getSelectedRanges());

    TermiteForest promisingTrees = analysisPhase(bm, ev);
    RegexNode candidate = synthesisPhase(bm, ev, promisingTrees);

    if (candidate != null) {
      printHeader("Results in the expression:");
      printRegex(candidate);

      printHeader("That matches the strings:");
      for (Range rng : getMatchingRanges(bm.getCorpus(), candidate)) {
        String str = getMatchingString(bm.getCorpus(), rng);
        printMatchStatus(true, rng, str);
      }
    }

    printHeader("All done");
  }

  static TermiteForest analysisPhase (Benchmark bm, Evaluator ev) {
    printHeader("Given the regular expression:");
    printRegex(bm.getOriginalRegex());

    printHeader("That already matches the strings:");
    for (int i = 0; i < ev.getOriginalRanges().size(); i++) {
      String   str = ev.getOriginalStrings().get(i);
      Range    rng = ev.getOriginalRanges().get(i);
      boolean isOK = ev.getSelectedStrings().contains(str);
      printMatchStatus(isOK, rng, str);
    }

    printHeader("When it *should only* match the strings:");
    for (int i = 0; i < ev.getSelectedRanges().size(); i++) {
      String   str = ev.getSelectedStrings().get(i);
      Range    rng = ev.getSelectedRanges().get(i);
      boolean isOK = ev.getOriginalStrings().contains(str);
      printMatchStatus(isOK, rng, str);
    }

    TermiteForest whole = Termite.digest(bm.getOriginalRegex());
    TermiteForest pruned = ev.pruneForest(whole);

    printHeader("Start by identifying promising sub-expressions:");
    for (TermiteTree tree : pruned.getTrees()) {
      printRegex(tree);
    }

    return pruned;
  }

  private static RegexNode synthesisPhase (Benchmark bm, Evaluator ev, TermiteForest candidates) {
    printHeader("Then synthesize character class replacements:");
    for (TermiteTree candidate : candidates.getTrees()) {
      RegexNode result = synthesizeTree(bm, ev, candidate);

      if (result != null) {
        return result;
      }
    }

    return null;
  }

  private static RegexNode synthesizeTree (Benchmark bm, Evaluator ev, TermiteTree tree) {
    // 1. assume all holes can be filled with a character class
    // 2. replace all holes with a character class (start with dot then refine)
    // 3. if class DOES NOT match all P, the hole must be more complex than
    //    a single character class, so goto SPLIT
    // 4. if class matches all P and rejects all N, goto DONE
    // 5. choose more restrictive class, then goto #2
    //
    // SPLIT: choose a compound node { Concat, Star, Plus, Union } and use the
    //    compound node to add 1 or more progeny holes and repeat from #2 for
    //    each of the new holes
    //
    // DONE: return synthesized regular expression

    CharClassSuggestEngine suggest = new CharClassSuggestEngine();
    RegexNode twig = null;

    whileLoop:
    while (true) {
      if ((twig = suggest.nextCharClass()) == null) {
        break whileLoop;
      }

      tree.fillHole(twig);

      if (ev.runPositiveExampleTest(tree.getRoot())) {
        // Regex matches all positive examples.
        if (ev.runNegativeExampleTest(tree.getRoot())) {
          // Regex rejects all negative examples.
          printRegexStatus(true, tree);
          return tree.getRoot();
        }
      } else {
        // Replace hole with some more complex regex node since P can't be
        // satisfied by a single character class.
        System.err.printf("%s needs to be expanded\n", tree.toString());
        System.exit(1);
      }

      printRegexStatus(false, tree);
    }

    return null;
  }

  private static void printHeader (String str) {
    System.out.printf("\n%s\n\n", str);
  }

  private static void printRegex (RegexNode reg) {
    Ansi.Cyan.printf("  %s\n", reg.toString());
  }

  private static void printMatchStatus (boolean isOK, Range rng, String str) {
    String fmt = "  %s %-8s %s\n";

    if (isOK) {
      Ansi.Green.printf(fmt, "✓", rng, str);
    } else {
      Ansi.Red.printf(fmt, "✗", rng, str);
    }
  }

  private static void printRegexStatus (boolean isOK, RegexNode reg) {
    String fmt = "  %s";

    if (isOK) {
      Ansi.Green.printf(fmt, "✓");
    } else {
      Ansi.Red.printf(fmt, "✗");
    }

    Ansi.Cyan.printf(" %s\n", reg.toString());
  }
}
