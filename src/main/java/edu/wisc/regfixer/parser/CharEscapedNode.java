package edu.wisc.regfixer.parser;

import java.util.Objects;

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
  public int hashCode () {
    return Objects.hash(this.toString());
  }

  @Override
  public boolean equals (CharClass other) {
    if (other instanceof CharEscapedNode) {
      return (this.ch == ((CharEscapedNode) other).getChar());
    }

    return false;
  }

  @Override
  public boolean equals (String other) {
    return this.toString().equals(other);
  }

  public String toString () {
    return String.format("\\%c", this.ch);
  }
}
