package edu.wisc.regfixer.parser;

public class RepetitionNode implements RegexNode {
  private RegexNode child;
  private int min;
  private int max;
  private boolean hasMax;

  public RepetitionNode (RegexNode child, int min) {
    this.child = child;
    this.min = min;
    this.hasMax = false;
  }

  public RepetitionNode (RegexNode child, int min, int max) {
    this.child = child;
    this.min = min;
    this.max = max;
    this.hasMax = true;
  }

  public RegexNode getChild () {
    return this.child;
  }

  public int getMin () {
    return this.min;
  }

  public boolean hasMax () {
    return this.hasMax;
  }

  public int getMax () {
    return this.max;
  }

  public String toString () {
    if (this.hasMax) {
      if (this.min == this.max) {
        return String.format("(%s){%d}", this.child, this.min);
      } else {
        return String.format("(%s){%d,%d}", this.child, this.min, this.max);
      }
    } else {
      return String.format("(%s){%d,}", this.child, this.min);
    }
  }
}
