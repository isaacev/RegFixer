package RegexParser;

public class MetaCharNode extends CharNode {
  public MetaCharNode (char ch) {
    super(ch);
  }

  public String toString () {
    return "\\" + Character.toString(this.ch);
  }
}
