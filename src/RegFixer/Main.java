package RegFixer;

import java.util.*;
import java.util.stream.*;
import RegexParser.*;

class Main {
  public static void main (String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: RegFixer <regex> ...<regex parts>");
      System.exit(1);
    }

    String twigs[] = new String[args.length - 1];
    for (int i = 1; i < args.length; i++) {
      twigs[i-1] = args[i];
    }

    runBatteries(args[0], twigs);
  }

  static void runBatteries (String treeRegex, String... twigRegexes) throws Exception {
    RegexNode tree = RegexParser.Main.parse(treeRegex);
    TermiteForest forest = Termite.digest(tree);

    for (int i = 0; i < twigRegexes.length; i++) {
      RegexNode twig = RegexParser.Main.parse(twigRegexes[i]);
      runBattery(forest, twig);
    }
  }

  static void runBattery (TermiteForest forest, RegexNode twig) throws Exception {
    // Print header.
    System.out.printf("\n%-20s%s\n", "regex:", "twig:");
    System.out.printf("%-20s%s\n", forest.getTree(), twig);
    System.out.println("----------------------------------------");

    // Graft a new twig at the hole location in each digested tree.
    List<RegexNode> graftedTrees = forest.getTrees().stream()
      .map(elem -> Grafter.graft(elem, twig))
      .collect(Collectors.toList());

    // Print comparison of digested tree with grafted tree.
    for (int i = 0; i < graftedTrees.size(); i++) {
      TermiteTree digestedTree = forest.getTrees().get(i);
      RegexNode graftedTree = graftedTrees.get(i);
      System.out.printf("%-21s%s\n", digestedTree, graftedTree);
    }
  }
}
