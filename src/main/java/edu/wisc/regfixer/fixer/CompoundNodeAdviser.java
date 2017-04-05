package edu.wisc.regfixer.fixer;

import java.util.Comparator;
import java.util.PriorityQueue;
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
  private PriorityQueue<RegexNode> suggestions;

  public CompoundNodeAdviser () {
    Comparator<RegexNode> comp = new CompoundNodeComparator();
    this.suggestions = new PriorityQueue<RegexNode>(comp);
  }

  public RegexNode nextSuggestion () {
    return null;
  }
}
