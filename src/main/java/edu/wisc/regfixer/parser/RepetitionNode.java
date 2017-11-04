package edu.wisc.regfixer.parser;

public class RepetitionNode implements RegexNode {
  private RegexNode child;
  private Bounds bounds;

  public RepetitionNode (RegexNode child, int min) {
    this.child = child;
    this.bounds = Bounds.atLeast(min);
  }

  public RepetitionNode (RegexNode child, int min, int max) {
    this.child = child;
    this.bounds = Bounds.between(min, max);
  }

  public RepetitionNode (RegexNode child, Bounds bounds) {
    this.child = child;
    this.bounds = bounds;
  }

  public RegexNode getChild () {
    return this.child;
  }

  public Bounds getBounds () {
    return this.bounds;
  }

  public int descendants () {
    return 1 + this.child.descendants();
  }

  public String toString () {
    return String.format("(%s)%s", this.child, this.bounds);
  }
}
