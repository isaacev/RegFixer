package edu.wisc.regfixer.parser;

import edu.wisc.regfixer.enumerate.UnknownInt;

public class RepetitionNode implements RegexNode {
  private RegexNode child;
  private int min;
  private int max;
  private boolean hasMax;
  private UnknownInt unknown;

  public RepetitionNode (RegexNode child, int min) {
    this.child = child;
    this.min = min;
    this.hasMax = false;
    this.unknown = null;
  }

  public RepetitionNode (RegexNode child, int min, int max) {
    this.child = child;
    this.min = min;
    this.max = max;
    this.hasMax = true;
    this.unknown = null;
  }

  public RepetitionNode (RegexNode child, UnknownInt unknown) {
    this.child = child;
    this.min = 0;
    this.max = 0;
    this.hasMax = true;
    this.unknown = unknown;
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

  public boolean hasUnknownBound () {
    if (this.unknown != null) {
      return true;
    }

    return false;
  }

  public UnknownInt getUnknownBound () {
    return this.unknown;
  }

  public int descendants () {
    return 1 + this.child.descendants();
  }

  public String toString () {
    if (this.hasUnknownBound()) {
      return String.format("(%s){%s}", this.child, this.unknown);
    }

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
