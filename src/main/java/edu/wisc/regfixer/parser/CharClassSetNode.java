package edu.wisc.regfixer.parser;

import java.util.List;

public class CharClassSetNode implements CharClass {
  private boolean not;
  private List<CharClass> subClasses;

  public CharClassSetNode (boolean not, List<CharClass> subClasses) {
    this.not = not;
    this.subClasses = subClasses;
  }

  public void addSubClass (CharClass subClass) {
    this.subClasses.add(subClass);
  }

  public String toString () {
    String str = this.subClasses.stream()
      .map(elem -> elem.toString())
      .reduce("", String::concat);

    if (this.not) {
      return String.format("[^%s]", str);
    } else {
      return String.format("[%s]", str);
    }
  }
}
