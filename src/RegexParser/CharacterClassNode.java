package RegexParser;

import java.util.List;

public class CharacterClassNode implements RegexNode {
  private Boolean except;
  private List<IntervalNode> children;

  public CharacterClassNode (Boolean except, List<IntervalNode> children){
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
