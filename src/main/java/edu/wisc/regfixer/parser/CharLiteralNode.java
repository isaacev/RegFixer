package edu.wisc.regfixer.parser;

import java.util.Objects;

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
  public int hashCode () {
    return Objects.hash(this.ch);
  }

  @Override
  public boolean equals (Object obj) {
    if (obj instanceof CharLiteralNode) {
      return (this.ch == ((CharLiteralNode) obj).getChar());
    }

    return false;
  }

  public String toString () {
    return String.format("%c", this.ch);
  }
}
