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

  public String toString () {
    return String.format("%c", this.ch);
  }
}
