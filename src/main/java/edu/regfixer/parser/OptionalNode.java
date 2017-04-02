package edu.wisc.regfixer.parser;

public class OptionalNode extends QuantifierNode {
  public OptionalNode (RegexNode child) {
    super('?', child);
  }
}
