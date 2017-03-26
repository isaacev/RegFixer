package RegexParser;

public class StarNode extends QuantifierNode {
  public StarNode (RegexNode child) {
    super('*', child);
  }
}
