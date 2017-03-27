package RegFixer;

import RegexParser.RegexNode;

class DigestedTree {
  private RegexNode root;

  DigestedTree (RegexNode root) {
    this.root = root;
  }

  RegexNode getRoot () {
    return this.root;
  }

  public String toString () {
    return this.root.toString();
  }
}
