package RegexParser;

public class UnionNode implements RegexNode {
  private RegexNode left;
  private RegexNode right;

  public UnionNode (RegexNode left, RegexNode right) {
    this.left = left;
    this.right = right;
  }

  public RegexNode getLeftChild () {
    return this.left;
  }

  public RegexNode getRightChild () {
    return this.right;
  }

  public String toString () {
    return String.format("%s|%s", this.left.toString(), this.right.toString());
  }
}
