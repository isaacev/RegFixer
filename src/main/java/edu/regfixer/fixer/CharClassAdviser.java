package edu.wisc.regfixer.fixer;

import java.util.Comparator;
import java.util.PriorityQueue;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharEscapedNode;

public class CharClassAdviser {
  private PriorityQueue<CharClass> suggestions;

  public CharClassAdviser () {
    Comparator<CharClass> comp = new CharClassComparator();
    this.suggestions = new PriorityQueue<CharClass>(comp);

    // Default starting character classes.
    this.suggestions.add(new CharDotNode());
    this.suggestions.add(new CharEscapedNode('w'));
    this.suggestions.add(new CharEscapedNode('d'));
  }

  public CharClass nextSuggestion () {
    return this.suggestions.poll();
  }
}
