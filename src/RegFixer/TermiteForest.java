package RegFixer;

import java.util.*;
import RegexParser.RegexNode;

class TermiteForest {
  private RegexNode tree;
  private List<TermiteTree> trees;

  TermiteForest (RegexNode tree, List<TermiteTree> trees) {
    this.tree = tree;
    this.trees = trees;
  }

  RegexNode getTree () {
    return this.tree;
  }

  List<TermiteTree> getTrees () {
    return this.trees;
  }
}
