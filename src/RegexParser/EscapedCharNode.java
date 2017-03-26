package RegexParser;

public class EscapedCharNode extends CharNode {
  public EscapedCharNode (char ch) {
    super(ch);
  }

  public String toString () {
    return "\\" + Character.toString(this.ch);
  }
}
