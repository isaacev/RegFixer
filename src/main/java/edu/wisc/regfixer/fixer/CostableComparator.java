package edu.wisc.regfixer.fixer;

import java.util.Comparator;

public class CostableComparator implements Comparator<Costable> {
  @Override
  public int compare (Costable a, Costable b) {
    return -Integer.compare(a.getCost(), b.getCost());
  }
}
