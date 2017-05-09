package edu.wisc.regfixer.automata;

import java.util.Map;
import java.util.Set;

public class Route {
  private Map<Integer, Set<Character>> spans;

  public Route (Map<Integer, Set<Character>> spans) {
    this.spans = spans;
  }

  public Map<Integer, Set<Character>> getSpans () {
    return this.spans;
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

    for (Map.Entry<Integer, Set<Character>> entry : this.spans.entrySet()) {
      accum += String.format("\tH%d {", entry.getKey());
      for (Character ch : entry.getValue()) {
        accum += String.format(" %c", ch);
      }
      accum += " }";
    }

    return accum;
  }
}
