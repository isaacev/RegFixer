package edu.wisc.regfixer.parser;

public class CharDotNode implements CharClass {
  public int descendants () {
    return 1;
  }

  public String toString () {
    return ".";
  }
}
