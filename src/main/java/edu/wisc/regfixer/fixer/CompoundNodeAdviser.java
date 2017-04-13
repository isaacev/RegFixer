package edu.wisc.regfixer.fixer;

import java.util.Queue;
import java.util.LinkedList;
import edu.wisc.regfixer.parser.RegexNode;

/**
 * CompoundNodeAdviser is responsible for making a recommendation for the next
 * compound expression node that should be inserted into an incomplete tree for
 * further synthesis. The candidates can be (in no particular order):
 *
 * - ConcatNode
 * - UnionNode
 * - StarNode
 * - PlusNode
 */
public class CompoundNodeAdviser {
  private Queue<RegexNode> suggestions;

  public CompoundNodeAdviser () {
    this.suggestions = new LinkedList<RegexNode>();
  }

  public RegexNode nextSuggestion () {
    return null;
  }
}
