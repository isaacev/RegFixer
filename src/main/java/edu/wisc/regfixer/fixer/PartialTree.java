package edu.wisc.regfixer.fixer;

import edu.wisc.regfixer.parser.RegexNode;

public class PartialTree implements RegexNode {
  private final RegexNode tree;
  private final HoleNode hole;

  public PartialTree (RegexNode tree, HoleNode hole) {
    this.tree = tree;
    this.hole = hole;
  }

  public RegexNode getTree () {
    return this.tree;
  }

  public HoleNode getHole () {
    return this.hole;
  }

  public void completeWith (RegexNode node) {
    this.hole.fill(node);
  }

  public void emptyHole () {
    this.hole.empty();
  }

  public String toString () {
    return this.tree.toString();
  }
}
