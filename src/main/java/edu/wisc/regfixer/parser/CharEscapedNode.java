package edu.wisc.regfixer.parser;

public class CharEscapedNode implements CharClass {
  private char ch;

  public CharEscapedNode (char ch) {
    this.ch = ch;
  }

  public char getChar () {
    return this.ch;
  }

  public String toString () {
    return String.format("\\%c", this.ch);
  }
}
