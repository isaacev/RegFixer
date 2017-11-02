package edu.wisc.regfixer.automata;

import java.util.Map;
import java.util.Set;

import edu.wisc.regfixer.enumerate.UnknownId;

public class Route {
  private Map<UnknownId, Set<Character>> spans;

  public Route (Map<UnknownId, Set<Character>> spans) {
    this.spans = spans;
  }

  public Map<UnknownId, Set<Character>> getSpans () {
    return this.spans;
  }

  public boolean isEmpty () {
    return this.spans.isEmpty();
  }

  @Override
  public boolean equals (Object other) {
    if (other instanceof Route) {
      return this.spans.equals(((Route) other).spans);
    }

    return false;
  }

  @Override
  public int hashCode () {
    return this.spans.hashCode();
  }

  @Override
  public String toString () {
    String accum = "";

    for (Map.Entry<UnknownId, Set<Character>> entry : this.spans.entrySet()) {
      if (accum.equals("") == false) {
        accum += "\n  ";
      }

      accum += String.format(" %s {", entry.getKey());

      for (Character ch : entry.getValue()) {
        accum += String.format(" %c", ch);
      }
      accum += " }";
    }

    return accum;
  }
}
