package edu.wisc.regfixer.parser;

public class CharLiteralNode implements ConcreteCharClass {
  private char ch;

  public CharLiteralNode (char ch) {
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
    if (other instanceof CharLiteralNode) {
      return (this.ch == ((CharLiteralNode) other).getChar());
    }

    return false;
  }

  @Override
  public boolean equals (String other) {
    return this.toString().equals(other);
  }

  public String toString () {
    return String.format("%c", this.ch);
  }
}
