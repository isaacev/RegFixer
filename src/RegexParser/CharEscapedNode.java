package RegexParser;

public class CharEscapedNode implements CharClass {
  private char ch;

  public CharEscapedNode (char ch) {
    this.ch = ch;
  }

  public int getBreadth () {
    return 0;
  }

  public String toString () {
    return String.format("\\%c", this.ch);
  }
}
