package RegFixer;

import java.util.*;
import java.util.stream.*;
import RegexParser.*;

class Main {
  public static void main (String[] args) {
    runBatteries("ab*c", "x*", "y", "z");
  }

  static void runBatteries (String treeRegex, String... branchRegexes) {
    for (int i = 0; i < branchRegexes.length; i++) {
      try {
        runBattery(treeRegex, branchRegexes[i]);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(1);
      }
    }
  }

  static void runBattery (String treeRegex, String branchRegex) throws Exception {
    RegexNode tree = RegexParser.Main.parse(treeRegex);
    RegexNode branch = RegexParser.Main.parse(branchRegex);

    // Print header.
    System.out.println("\nregex:\t\tbranch:");
    System.out.printf("%s\t\t%s\n", tree, branch);
    System.out.println("--------------------------------");

    // Add holes to all possible locations in tree.
    List<DigestedTree> digestedTrees = Termite.digest(tree);

    // Graft a new branch at each hole location in each digested tree.
    List<RegexNode> graftedTrees = digestedTrees.stream()
      .map(elem -> Grafter.graft(elem, branch))
      .collect(Collectors.toList());

    // Print comparison of digested tree with grafted tree.
    for (int i = 0; i < graftedTrees.size(); i++) {
      DigestedTree digestedTree = digestedTrees.get(i);
      RegexNode graftedTree = graftedTrees.get(i);
      System.out.printf("%s\t\t%s\n", digestedTree, graftedTree);
    }
  }
}
