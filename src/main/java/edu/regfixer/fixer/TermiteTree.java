package edu.wisc.regfixer.fixer;

import edu.wisc.regfixer.parser.*;

public class TermiteTree implements RegexNode {
  private RegexNode root;
  private HoleNode hole;

  public TermiteTree (RegexNode root, HoleNode hole) {
    this.root = root;
    this.hole = hole;
  }

  public RegexNode getRoot () {
    return this.root;
  }

  public void fillHole (RegexNode node) {
    this.hole.fill(node);
  }

  public void emptyHole () {
    this.hole.empty();
  }

  public String toString () {
    return this.root.toString();
  }
}
