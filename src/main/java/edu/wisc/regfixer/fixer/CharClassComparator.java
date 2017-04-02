package edu.wisc.regfixer.fixer;

import java.util.Comparator;
import edu.wisc.regfixer.parser.CharClass;

public class CharClassComparator implements Comparator<CharClass> {
  @Override
  public int compare (CharClass a, CharClass b) {
    return b.getBreadth() - a.getBreadth();
  }
}
