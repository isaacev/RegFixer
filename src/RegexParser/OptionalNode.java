package RegexParser;

public class OptionalNode extends QuantifierNode {
  public OptionalNode (RegexNode child) {
    super('?', child);
  }
}
