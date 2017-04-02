package edu.wisc.regfixer.parser;

public class PlusNode extends QuantifierNode {
  public PlusNode (RegexNode child) {
    super('+', child);
  }
}
