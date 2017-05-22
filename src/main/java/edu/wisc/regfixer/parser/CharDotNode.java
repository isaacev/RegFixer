package edu.wisc.regfixer.parser;

public class CharDotNode implements CharClass {
  public int descendants () {
    return 1;
  }

  @Override
  public boolean equals (CharClass other) {
    return (other instanceof CharDotNode);
  }

  public String toString () {
    return ".";
  }
}
