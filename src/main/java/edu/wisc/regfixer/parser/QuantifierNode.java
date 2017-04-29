package edu.wisc.regfixer.parser;

public abstract class QuantifierNode implements RegexNode {
  private char operator;
  private RegexNode child;

  public QuantifierNode (char operator, RegexNode child) {
    this.operator = operator;
    this.child = child;
  }

  public RegexNode getChild () {
    return this.child;
  }

  public int descendants () {
    return 1 + this.child.descendants();
  }

  public String toString () {
    return "(" + this.child + ")" + Character.toString(this.operator);
  }
}
