package edu.wisc.regfixer.parser;

public class CharEscapedNode implements CharClass {
  private char ch;

  public CharEscapedNode (char ch) {
    this.ch = ch;
  }

  public int getBreadth () {
    return 200;
  }

  public String toString () {
    return String.format("\\%c", this.ch);
  }
}
