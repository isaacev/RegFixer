package edu.wisc.regfixer.parser;

public class CharLiteralNode implements CharClass {
  private char ch;

  public CharLiteralNode (char ch) {
    this.ch = ch;
  }

  public char getChar () {
    return this.ch;
  }

  public String toString () {
    return String.format("%c", this.ch);
  }
}
