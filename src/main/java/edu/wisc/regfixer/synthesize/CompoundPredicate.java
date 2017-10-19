package edu.wisc.regfixer.synthesize;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

public class CompoundPredicate implements Predicate {
  private Set<Predicate> components;
  private boolean inclusive;

  public CompoundPredicate (Predicate... components) {
    this.components = new HashSet<>(Arrays.asList(components));
    this.inclusive = true;
  }

  public CompoundPredicate (boolean inclusive, Predicate... components) {
    this.components = new HashSet<>(Arrays.asList(components));
    this.inclusive = inclusive;
  }

  public Set<Predicate> getComponents () {
    return this.components;
  }

  public boolean equals (Predicate any) {
    // TODO
    return false;
  }

  public boolean includes (Predicate other) {
    // TODO
    return false;
  }

  public boolean includes (char other) {
    // TODO
    return false;
  }
}
