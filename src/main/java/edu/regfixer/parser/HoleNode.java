package edu.wisc.regfixer.parser;

public class HoleNode implements RegexNode {
  private RegexNode child;

  public HoleNode () {
    this.child = null;
  }

  public HoleNode (RegexNode child) {
    this.child = child;
  }

  public void fill (RegexNode child) {
    this.child = child;
  }

  public void empty () {
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
