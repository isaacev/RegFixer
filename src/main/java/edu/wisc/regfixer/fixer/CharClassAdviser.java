package edu.wisc.regfixer.fixer;

import java.util.Queue;
import java.util.LinkedList;
import edu.wisc.regfixer.parser.CharClass;
import edu.wisc.regfixer.parser.CharDotNode;
import edu.wisc.regfixer.parser.CharEscapedNode;

/**
 * CharClassAdviser is responsible for making a recommendation for the next
 * character class that should be inserted into an incomplete tree during
 * expression synthesis.
 */
public class CharClassAdviser {
  private Queue<CharClass> suggestions;

  public CharClassAdviser () {
    this.suggestions = new LinkedList<CharClass>();

    this.suggestions.add(new CharDotNode());
    this.suggestions.add(new CharEscapedNode('w'));
    this.suggestions.add(new CharEscapedNode('d'));
  }

  public CharClass nextSuggestion () {
    return this.suggestions.poll();
  }
}
