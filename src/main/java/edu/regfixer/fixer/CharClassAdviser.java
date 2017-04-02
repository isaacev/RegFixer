package edu.wisc.regfixer.fixer;

import java.util.Comparator;
import java.util.PriorityQueue;
import edu.wisc.regfixer.parser.CharClass;

public class CharClassAdviser {
  private PriorityQueue<CharClass> suggestions;

  public CharClassAdviser () {
    Comparator<CharClass> comp = new CharClassComparator();
    this.suggestions = new PriorityQueue<CharClass>(comp);
  }

  public CharClass nextSuggestion () {
    return null;
  }
}
