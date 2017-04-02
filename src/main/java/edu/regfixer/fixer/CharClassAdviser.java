package edu.wisc.regfixer.fixer;

import java.util.*;
import edu.wisc.regfixer.parser.*;

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
