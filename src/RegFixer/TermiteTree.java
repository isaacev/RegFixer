package RegFixer;

import RegexParser.RegexNode;

class TermiteTree {
  private RegexNode root;

  TermiteTree (RegexNode root) {
    this.root = root;
  }

  RegexNode getRoot () {
    return this.root;
  }

  public String toString () {
    return this.root.toString();
  }
}
