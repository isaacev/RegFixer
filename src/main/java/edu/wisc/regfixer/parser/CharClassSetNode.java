package edu.wisc.regfixer.parser;

import java.util.List;

public class CharClassSetNode implements CharClass {
  private boolean not;
  private List<CharRangeNode> subClasses;

  public CharClassSetNode (boolean not, List<CharRangeNode> subClasses) {
    this.not = not;
    this.subClasses = subClasses;
  }

  public boolean isInverted () {
    return this.not;
  }

  public List<CharRangeNode> getSubClasses () {
    return this.subClasses;
  }

  public void addSubClass (CharRangeNode subClass) {
    this.subClasses.add(subClass);
  }

  public int descendants () {
    return 1;
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
