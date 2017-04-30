package edu.wisc.regfixer.fixer;

import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.RegexNode;
import edu.wisc.regfixer.parser.StarNode;
import edu.wisc.regfixer.parser.CharLiteralNode;

public class HoleNode implements RegexNode, Costable {
  private final int removedNodes;
  private final int addedNodes;
  private RegexNode child = null;

  public HoleNode (int removedNodes) {
    this(removedNodes, 0);
  }

  public HoleNode (int removedNodes, int addedNodes) {
    this.removedNodes = removedNodes;
    this.addedNodes = addedNodes;
  }

  public int getRemovedNodes () {
    return this.removedNodes;
  }

  public int getAddedNodes () {
    return this.addedNodes;
  }

  public int getCost () {
    return -this.getAddedNodes() - this.getRemovedNodes();
  }

  public int descendants () {
    return 0;
  }

  public void fill (RegexNode child) {
    this.child = child;
  }

  public void clear () {
    this.child = null;
  }

  public String toString () {
    if (this.child == null) {
      return "❑";
    } else {
      return this.child.toString();
    }
  }
}
