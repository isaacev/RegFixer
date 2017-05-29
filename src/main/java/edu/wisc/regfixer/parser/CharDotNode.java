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
  public boolean equals (Object obj) {
    return (obj instanceof CharDotNode);
  }

  public String toString () {
    return ".";
  }
}
