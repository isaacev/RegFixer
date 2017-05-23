package edu.wisc.regfixer.parser;

public interface CharClass extends RegexNode {
  public boolean equals (CharClass other);
  public boolean equals (String other);
}
