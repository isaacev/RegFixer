package edu.wisc.regfixer.parser;

import java.util.Objects;

public class CharDotNode implements CharClass {
  public int descendants () {
    return 1;
  }

  @Override
  public int hashCode () {
    return Objects.hash(".");
  }

  @Override
  public boolean equals (CharClass other) {
    return (other instanceof CharDotNode);
  }

  @Override
  public boolean equals (String other) {
    return other.equals(".");
  }

  public String toString () {
    return ".";
  }
}
