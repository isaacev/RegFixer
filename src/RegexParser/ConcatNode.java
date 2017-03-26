package RegexParser;

import java.util.List;

public class ConcatNode implements RegexNode {
  private List<RegexNode> children;

  public ConcatNode (List<RegexNode> children) {
    this.children = children;
  }

  public List<RegexNode> getChildren () {
    return this.children;
  }

  public String toString () {
    String out = "";

    for (RegexNode child : this.children) {
      out += child.toString();
    }

    return out;
  }
}
