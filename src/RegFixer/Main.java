package RegFixer;

import java.util.List;
import RegexParser.RegexNode;
import RegexParser.RegexParserProvider;

public class Main {
  public static void main (String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: java RegFixer <regex>");
      System.exit(1);
    }

    // Parse a string to a regex AST.
    RegexParserProvider prov = new RegexParserProvider(args[0]);
    RegexNode regex = prov.process();

    // Add holes to the AST.
    List<RegexNode> digestedTrees = Termite.digest(regex);

    // Print holed ASTs.
    if (digestedTrees == null || digestedTrees.size() == 0) {
      System.out.println("no trees returned");
    } else {
      for (RegexNode tree : digestedTrees) {
        System.out.println(tree.toCleanString());
      }
    }
  }
}
