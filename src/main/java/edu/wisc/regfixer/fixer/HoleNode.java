package edu.wisc.regfixer.fixer;

import edu.wisc.regfixer.parser.RegexNode;

public class HoleNode implements RegexNode, Costable {
  private final int removedNodes;
  private final int addedNodes = 0;

  public HoleNode (int removedNodes) {
    this.removedNodes = removedNodes;
  }

  public int getRemovedNodes () {
    return this.removedNodes;
  }

  public int getAddedNodes () {
    return this.addedNodes;
  }

  public int getCost () {
    return this.getAddedNodes() - this.getRemovedNodes();
  }

  public int descendants () {
    return 0;
  }

  public String toString () {
    return "❑";
  }
}
