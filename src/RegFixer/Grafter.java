package RegFixer;

import RegexParser.RegexNode;

/**
 * Grafter is responsible for taking a regular expression abstract syntax tree
 * with a hole and attach some other regular expression tree at that location to
 * produce a larger AST with no holes.
 */
public class Grafter {
  static void graft (RegexNode tree, RegexNode branch) {
    System.err.println("Grafter.graft not implemented yet");
    System.exit(1);
  }
}
