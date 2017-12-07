package edu.wisc.regfixer.parser;

import java.util.Arrays;
import java.util.List;

public class ConcatNode implements RegexNode {
  private List<RegexNode> children;

  public ConcatNode (List<RegexNode> children) {
    this.children = children;
  }

  public ConcatNode (RegexNode... children) {
    this.children = Arrays.asList(children);
  }

  public List<RegexNode> getChildren () {
    return this.children;
  }

  public int descendants () {
    return this.children.stream().mapToInt(RegexNode::descendants).sum();
  }

  public String toString () {
    String out = "";

    for (RegexNode child : this.children) {
      if (child instanceof ConcatNode) {
        out += "(" + child.toString() + ")";
      } else {
        out += child.toString();
      }
    }

    return out;
  }
}
