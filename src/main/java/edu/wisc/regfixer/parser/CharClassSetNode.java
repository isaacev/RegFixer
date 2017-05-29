package edu.wisc.regfixer.parser;

import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

public class CharClassSetNode implements CharClass {
  private boolean not;
  private Set<CharRangeNode> subClasses;

  public CharClassSetNode (boolean not, Collection<CharRangeNode> subClasses) {
    this.not = not;
    this.subClasses = new HashSet<>(subClasses);
  }

  public boolean isInverted () {
    return this.not;
  }

  public Set<CharRangeNode> getSubClasses () {
    return this.subClasses;
  }

  public void addSubClass (CharRangeNode subClass) {
    this.subClasses.add(subClass);
  }

  public int descendants () {
    return 1;
  }

  @Override
  public int hashCode () {
    return this.subClasses.stream().mapToInt(c -> c.hashCode()).sum();
  }

  @Override
  public boolean equals (Object obj) {
    if (obj instanceof CharClassSetNode) {
      CharClassSetNode cast = (CharClassSetNode) obj;
      boolean sameSubClasses = this.subClasses.equals(cast.getSubClasses());
      boolean sameNegation = (this.not == cast.isInverted());
      return (sameSubClasses && sameNegation);
    }

    return false;
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
