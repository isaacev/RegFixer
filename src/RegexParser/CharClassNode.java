package RegexParser;

import java.util.List;

public class CharClassNode implements RegexNode {
  private Boolean except;
  private List<IntervalNode> children;

  public CharClassNode (Boolean except, List<IntervalNode> children){
    this.except = except;
    this.children = children;
  }

  void addInterval (IntervalNode child) {
    this.children.add(child);
  }

  public String toString () {
    String out = "";

    for (RegexNode child : this.children) {
      out += child.toString();
    }

    return "[" + (this.except ? "^" : "") + out + "]";
  }
}
