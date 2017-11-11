package edu.wisc.regfixer.parser;

public class UnionNode implements RegexNode {
  private RegexNode left;
  private RegexNode right;
  private boolean synthetic;

  public UnionNode (RegexNode left, RegexNode right) {
    this.left = left;
    this.right = right;
    this.synthetic = false;
  }

  public UnionNode (RegexNode left, RegexNode right, boolean isSynthetic) {
    this.left = left;
    this.right = right;
    this.synthetic = isSynthetic;
  }

  public RegexNode getLeftChild () {
    return this.left;
  }

  public RegexNode getRightChild () {
    return this.right;
  }

  public boolean isSynthetic () {
    return this.synthetic;
  }

  public int descendants () {
    return 1 + this.left.descendants() + this.right.descendants();
  }

  public String toString () {
    return String.format("%s|%s", this.left.toString(), this.right.toString());
  }
}
