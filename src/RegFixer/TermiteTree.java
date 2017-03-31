package RegFixer;

import RegexParser.*;

class TermiteTree implements RegexNode {
  private RegexNode root;
  private HoleNode hole;

  TermiteTree (RegexNode root, HoleNode hole) {
    this.root = root;
    this.hole = hole;
  }

  RegexNode getRoot () {
    return this.root;
  }

  void fillHole (RegexNode node) {
    this.hole.fill(node);
  }

  void emptyHole () {
    this.hole.empty();
  }

  public String toString () {
    return this.root.toString();
  }
}
