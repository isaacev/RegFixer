package edu.wisc.regfixer.parser;

public class CharEscapedNode implements ConcreteCharClass {
  private char ch;

  public CharEscapedNode (char ch) {
    this.ch = ch;
  }

  public char getChar () {
    return this.ch;
  }

  public int descendants () {
    return 1;
  }

  @Override
  public boolean equals (CharClass other) {
    if (other instanceof CharEscapedNode) {
      return (this.ch == ((CharEscapedNode) other).getChar());
    }

    return false;
  }

  public String toString () {
    return String.format("\\%c", this.ch);
  }
}
