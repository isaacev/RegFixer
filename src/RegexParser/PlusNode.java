package RegexParser;

public class PlusNode extends QuantifierNode {
  public PlusNode (RegexNode child) {
    super('+', child);
  }
}
