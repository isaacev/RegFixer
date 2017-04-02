package edu.wisc.regfixer.parser;

public class StarNode extends QuantifierNode {
  public StarNode (RegexNode child) {
    super('*', child);
  }
}
