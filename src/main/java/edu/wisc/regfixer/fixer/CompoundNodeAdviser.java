package edu.wisc.regfixer.fixer;

import java.util.Comparator;
import java.util.PriorityQueue;
import edu.wisc.regfixer.parser.RegexNode;

public class CompoundNodeAdviser {
  private PriorityQueue<RegexNode> suggestions;

  public CompoundNodeAdviser () {
    Comparator<RegexNode> comp = new CompoundNodeComparator();
    this.suggestions = new PriorityQueue<RegexNode>(comp);
  }

  public RegexNode nextSuggestion () {
    return null;
  }
}
