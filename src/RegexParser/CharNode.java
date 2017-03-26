package RegexParser;

public class CharNode implements RegexNode {
  protected char ch;

  public CharNode (char ch) {
    this.ch = ch;
  }

  public String toString () {
    return Character.toString(this.ch);
  }
}
